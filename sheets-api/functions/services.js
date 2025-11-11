/* eslint-disable */
const admin = require("firebase-admin");
const {google} = require("googleapis");
const { logger } = require("firebase-functions");

const SCOPES = ["https://www.googleapis.com/auth/spreadsheets"];

async function getSheetsClient() {
  const auth = await google.auth.getClient({scopes: SCOPES});
  return google.sheets({version: "v4", auth});
}

async function getMedicationsByPetService({userId, petId}) {
  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();

  const [medsResp, maResp] = await Promise.all([
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "medications!A1:Z10000",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "medicationassignments!A1:Z10000",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
  ]);

  const mrows = medsResp.data.values || [];
  const arows = maResp.data.values || [];
  if (mrows.length <= 1) return [];

  const medById = new Map();
  for (let i = 1; i < mrows.length; i++) {
    const cols = mrows[i];
    const id = String(cols[0] || "");
    medById.set(id, {
      medication_id: id,
      user_id: String(cols[1] || ""),
      medication_name: String(cols[2] || ""),
      start_of_medication: String(cols[3] || ""), // map start_of_supply -> start_of_medication (app expects this name)
      take_every_number: String(cols[4] || ""),   // map perform_every_number -> take_every_number
      take_every_unit: String(cols[5] || ""),     // map perform_every_unit -> take_every_unit
      dose_number: String(cols[6] || ""),
      dose_unit: String(cols[7] || ""),
      total_doses: String(cols[8] || ""),
      created_at: String(cols[9] || ""),
    });
  }

  // medicationassignments columns
  // Current: A assignment_id, B medication_id, C pet_id, D user_id, E start_of_supply, F end_of_supply, G next_supply, H created_at, I is_completed
  // Legacy variants are not expected, but we still try to map by header names.
  const headers = Array.isArray(arows[0]) ? arows[0].map(h => String(h).trim().toLowerCase()) : [];
  const idx = {
    assignment_id: headers.indexOf("assignment_id"),
    medication_id: headers.indexOf("medication_id"),
    pet_id: headers.indexOf("pet_id"),
    user_id: headers.indexOf("user_id"),
    start_of_supply: headers.indexOf("start_of_supply"),
    end_of_supply: headers.indexOf("end_of_supply"),
    next_supply: headers.indexOf("next_supply"),
    created_at: headers.indexOf("created_at"),
    is_completed: headers.indexOf("is_completed"),
  };
  // Fallback to positional indices if header names are missing or have variants
  const ensureIdx = (cur, def) => (cur >= 0 ? cur : def);
  idx.assignment_id = ensureIdx(idx.assignment_id, 0);
  idx.medication_id = ensureIdx(idx.medication_id, 1);
  idx.pet_id = ensureIdx(idx.pet_id, 2);
  idx.user_id = ensureIdx(idx.user_id, 3);
  idx.start_of_supply = ensureIdx(idx.start_of_supply, 4);
  idx.end_of_supply = ensureIdx(idx.end_of_supply, 5);
  idx.next_supply = ensureIdx(idx.next_supply, 6);
  idx.created_at = ensureIdx(idx.created_at, 7);
  idx.is_completed = ensureIdx(idx.is_completed, 8);
  const out = [];
  for (let i = 1; i < arows.length; i++) {
    const cols = arows[i];
    if (String(cols[idx.pet_id]) === String(petId) && String(cols[idx.user_id]) === String(userId)) {
      const medicationId = String(cols[idx.medication_id] || "");
      const med = medById.get(medicationId);
      if (med && med.user_id === String(userId)) {
        const doseCombined = (med.dose_number ? String(med.dose_number) : "") + (med.dose_unit ? ` ${med.dose_unit}` : "");
        const startAssign = idx.start_of_supply >= 0 ? String(cols[idx.start_of_supply] || "") : ""; // epoch ms expected
        const endVal = idx.end_of_supply >= 0 ? String(cols[idx.end_of_supply] || "") : "";
        // Always compute next_dose = start_of_supply + 1 interval (ignore next_supply column for API output)
        let nextVal = "";
        {
          const startMs = Number(startAssign);
          const nInt = Number(med.take_every_number || 0);
          const unitStr = String(med.take_every_unit || "").toLowerCase();
          if (Number.isFinite(startMs) && startMs > 0 && Number.isFinite(nInt) && nInt > 0) {
            const d = new Date(startMs);
            switch (unitStr) {
              case "hour":
              case "hours": d.setHours(d.getHours() + nInt); break;
              case "day":
              case "days": d.setDate(d.getDate() + nInt); break;
              case "week":
              case "weeks": d.setDate(d.getDate() + 7 * nInt); break;
              case "month":
              case "months": d.setMonth(d.getMonth() + nInt); break;
            }
            nextVal = String(d.getTime());
          }
        }
        logger.info("LOGUEO_BUSCADO getMedications: row match", {
          pet_id: String(cols[idx.pet_id] || ""), medication_id: medicationId,
          start_raw: startAssign || med.start_of_medication, end_raw: endVal, next_raw: nextVal,
        });
        logger.info("LOGUEO_BUSCADO getMedications: assignment row", {
          assignment_id: String(cols[idx.assignment_id] || ""),
          cols,
        });
        out.push({
          assignment_id: String(cols[idx.assignment_id] || ""),
          pet_id: String(cols[idx.pet_id] || ""),
          user_id: String(cols[idx.user_id] || ""),
          end_of_supply: endVal,
          next_dose: nextVal,
          dose: doseCombined,
          ...med,
          start_of_medication: startAssign || med.start_of_medication,
        });
      }
    }
  }

  return out;
}

async function deleteMedicationService({ userId, body }) {
  const medicationId = required(body.medication_id || body.id, "medication_id");
  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();

  logger.info(`deleteMedication: iniciado para medicationId=${medicationId}, userId=${userId}`);
  logger.info(`deleteMedication: usando spreadsheetId=${spreadsheetId}`);

  // Cargar hojas
  const [medsResp, maResp] = await Promise.all([
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "medications!A1:Z500",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "medicationassignments!A1:Z500",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
  ]);

  const mrows = medsResp.data.values || [];
  const arows = maResp.data.values || [];
  if (mrows.length === 0) { const e = new Error("medications sheet missing"); e.status = 500; throw e; }
  if (arows.length === 0) { const e = new Error("medicationassignments sheet missing"); e.status = 500; throw e; }

  const clean = (str) => String(str || "").trim();

  // Filtrar medicamento
  const mHeader = mrows[0];
  const mFiltered = [mHeader, ...mrows.slice(1).filter(cols => clean(cols[0]) !== clean(medicationId))];

  // Filtrar asignaciones (col B medication_id)
  const aHeader = arows[0];
  const aFiltered = [aHeader, ...arows.slice(1).filter(cols => clean(cols[1]) !== clean(medicationId))];

  // Logs de comparación
  mrows.slice(1).forEach(cols => logger.info(`Comparando medication: "${clean(cols[0])}" con "${clean(medicationId)}"`));
  arows.slice(1).forEach(cols => logger.info(`Comparando med assignment: "${clean(cols[1])}" con "${clean(medicationId)}"`));

  const removedMeds = mrows.length - mFiltered.length;
  const removedAssignments = arows.length - aFiltered.length;
  logger.info(`deleteMedication: encontrados ${removedMeds} medications y ${removedAssignments} assignments para eliminar`);

  // Limpiar áreas y escribir
  await Promise.all([
    sheets.spreadsheets.values.clear({ spreadsheetId, range: "medications!A2:Z500" }),
    sheets.spreadsheets.values.clear({ spreadsheetId, range: "medicationassignments!A2:Z500" }),
  ]);

  logger.info("deleteMedication: cleared old data ranges before update");

  const activityId = admin.firestore().collection("_ids").doc().id;
  const now = isoNow();
  const details = JSON.stringify({ medication_id: medicationId });

  const [updateMedsResp, updateAssignmentsResp, appendLogResp] = await Promise.all([
    sheets.spreadsheets.values.update({
      spreadsheetId,
      range: "medications!A1",
      valueInputOption: "USER_ENTERED",
      requestBody: { values: mFiltered },
    }),
    sheets.spreadsheets.values.update({
      spreadsheetId,
      range: "medicationassignments!A1",
      valueInputOption: "USER_ENTERED",
      requestBody: { values: aFiltered },
    }),
    sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "activitylog!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: { values: [[activityId, userId, "DELETE_MEDICATION", "medications", medicationId, now, details]] },
    }),
  ]);

  logger.info(`Update meds response: ${JSON.stringify(updateMedsResp.data)}`);
  logger.info(`Update med-assignments response: ${JSON.stringify(updateAssignmentsResp.data)}`);
  logger.info(`Append log response: ${JSON.stringify(appendLogResp.data)}`);

  return { medication_id: medicationId, deleted: removedMeds > 0 || removedAssignments > 0, deleted_at: now };
}

async function deleteRoutineService({ userId, body }) {
  const routineId = required(body.routine_id || body.id, "routine_id");
  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();

  logger.info(`deleteRoutine: iniciado para routineId=${routineId}, userId=${userId}`);
  logger.info(`deleteRoutine: usando spreadsheetId=${spreadsheetId}`);

  // 🔹 Cargar hojas
  const [routinesResp, raResp] = await Promise.all([
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "routines!A1:Z500",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "routineassignments!A1:Z500",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
  ]);

  const rrows = routinesResp.data.values || [];
  const arows = raResp.data.values || [];

  if (rrows.length === 0) {
    const e = new Error("routines sheet missing");
    e.status = 500;
    throw e;
  }
  if (arows.length === 0) {
    const e = new Error("routineassignments sheet missing");
    e.status = 500;
    throw e;
  }

  // 🔹 Función auxiliar para limpiar valores
  const clean = str => String(str || "").trim();

  // 🔹 Filtrar rutina
  const rHeader = rrows[0];
  const rFiltered = [
    rHeader,
    ...rrows.slice(1).filter(cols => clean(cols[0]) !== clean(routineId)),
  ];

  // 🔹 Filtrar asignaciones
  const aHeader = arows[0];
  const aFiltered = [
    aHeader,
    ...arows.slice(1).filter(cols => clean(cols[1]) !== clean(routineId)),
  ];

  // 🔹 Logs de comparación (útiles para depurar)
  rrows.slice(1).forEach(cols => {
    logger.info(`Comparando rutina: "${clean(cols[0])}" con "${clean(routineId)}"`);
  });
  arows.slice(1).forEach(cols => {
    logger.info(`Comparando asignación: "${clean(cols[1])}" con "${clean(routineId)}"`);
  });

  const removedRoutines = rrows.length - rFiltered.length;
  const removedAssignments = arows.length - aFiltered.length;

  logger.info(
    `deleteRoutine: encontrados ${removedRoutines} routines y ${removedAssignments} assignments para eliminar`
  );

  // 🔹 Limpiar las áreas antes de escribir (para eliminar filas residuales)
  await Promise.all([
    sheets.spreadsheets.values.clear({
      spreadsheetId,
      range: "routines!A2:Z500",
    }),
    sheets.spreadsheets.values.clear({
      spreadsheetId,
      range: "routineassignments!A2:Z500",
    }),
  ]);

  logger.info("deleteRoutine: cleared old data ranges before update");

  // 🔹 Preparar datos para el log de actividad
  const activityId = admin.firestore().collection("_ids").doc().id;
  const now = isoNow();
  const details = JSON.stringify({ routine_id: routineId });

  // 🔹 Actualizar hojas y registrar actividad
  const [updateRoutinesResp, updateAssignmentsResp, appendLogResp] = await Promise.all([
    sheets.spreadsheets.values.update({
      spreadsheetId,
      range: "routines!A1",
      valueInputOption: "USER_ENTERED",
      requestBody: { values: rFiltered },
    }),
    sheets.spreadsheets.values.update({
      spreadsheetId,
      range: "routineassignments!A1",
      valueInputOption: "USER_ENTERED",
      requestBody: { values: aFiltered },
    }),
    sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "activitylog!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: {
        values: [
          [activityId, userId, "DELETE_ROUTINE", "routines", routineId, now, details],
        ],
      },
    }),
  ]);

  // 🔹 Logs detallados de resultados
  logger.info(`Update routines response: ${JSON.stringify(updateRoutinesResp.data)}`);
  logger.info(`Update assignments response: ${JSON.stringify(updateAssignmentsResp.data)}`);
  logger.info(`Append log response: ${JSON.stringify(appendLogResp.data)}`);

  logger.info(
    `deleteRoutine: removed ${removedRoutines} routines, ${removedAssignments} assignments for ${userId}`
  );
  logger.info(`deleteRoutine: rFiltered=${rFiltered.length}, aFiltered=${aFiltered.length}`);

  return {
    routine_id: routineId,
    deleted: removedRoutines > 0 || removedAssignments > 0,
    deleted_at: now,
  };
}

// Upsert user into users sheet: columns: user_id, email, name, created_at, last_login
async function createOrUpdateUserService({userId, email, name}) {
  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();
  const now = isoNow();

  // Load users sheet
  const resp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "users!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const rows = resp.data.values || [];
  // If only header or empty, just append new row
  if (rows.length <= 1) {
    await sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "users!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: {values: [[userId, email || "", name || "", now, now]]},
    });
    return {status: "created", user_id: userId};
  }
  // Find by user_id or email
  let foundIndex = -1;
  for (let i = 1; i < rows.length; i++) {
    const cols = rows[i];
    const uid = String(cols[0] || "");
    const em = String(cols[1] || "");
    if (uid === String(userId) || (email && em.toLowerCase() === String(email).toLowerCase())) {
      foundIndex = i; break;
    }
  }
  if (foundIndex === -1) {
    await sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "users!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: {values: [[userId, email || "", name || "", now, now]]},
    });
    return {status: "created", user_id: userId};
  }
  const rowNumber = foundIndex + 1;
  // Update name (if provided) and last_login
  const current = rows[foundIndex];
  const currentName = String(current[2] || "");
  const newName = name && String(name).trim() ? String(name).trim() : currentName;
  await sheets.spreadsheets.values.update({
    spreadsheetId,
    range: `users!C${rowNumber}:E${rowNumber}`,
    valueInputOption: "USER_ENTERED",
    requestBody: {values: [[newName, current[3] || now, now]]},
  });
  return {status: "updated", user_id: userId};
}

async function getRoutinesByPetService({userId, petId}) {
  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();

  // Load routines and assignments
  const [routinesResp, raResp] = await Promise.all([
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "routines!A1:Z10000",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "routineassignments!A1:Z10000",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
  ]);

  const rrows = routinesResp.data.values || [];
  const arows = raResp.data.values || [];
  if (rrows.length <= 1) return [];

  const toEpochString = (v) => {
    if (v === undefined || v === null) return "";
    if (typeof v === "number") return String(Math.round(v));
    const s = String(v).trim();
    if (s === "") return "";
    if (/^\d+(\.\d+)?$/.test(s)) return s; // numeric string
    const mIso = s.match(/^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})(?::(\d{2}))?/);
    if (mIso) {
      const yyyy = Number(mIso[1]);
      const mm = Number(mIso[2]);
      const dd = Number(mIso[3]);
      const HH = Number(mIso[4]);
      const MM = Number(mIso[5]);
      const SS = mIso[6] ? Number(mIso[6]) : 0;
      const dtLocal = new Date(yyyy, mm - 1, dd, HH, MM, SS, 0);
      return String(dtLocal.getTime());
    }
    return s;
  };

  const routineById = new Map();
  // routines columns: A routine_id, B user_id, C routine_name, D start_of_activity, E perform_every_number, F perform_every_unit, G created_at
  for (let i = 1; i < rrows.length; i++) {
    const cols = rrows[i];
    routineById.set(String(cols[0]), {
      routine_id: String(cols[0] || ""),
      user_id: String(cols[1] || ""),
      routine_name: String(cols[2] || ""),
      start_of_activity: toEpochString(cols[3]),
      perform_every_number: String(cols[4] || ""),
      perform_every_unit: String(cols[5] || ""),
      created_at: String(cols[6] || ""),
    });
  }

  // routineassignments columns: A assignment_id, B routine_id, C pet_id, D user_id, E assigned_at, F last_performed_at, G next_activity
  const out = [];
  for (let i = 1; i < arows.length; i++) {
    const cols = arows[i];
    if (String(cols[2]) === String(petId) && String(cols[3]) === String(userId)) {
      const routine = routineById.get(String(cols[1]));
      if (routine && routine.user_id === String(userId)) {
        out.push({
          assignment_id: String(cols[0] || ""),
          pet_id: String(cols[2] || ""),
          user_id: String(cols[3] || ""),
          last_performed_at: toEpochString(cols[5]),
          next_activity: toEpochString(cols[6]),
          ...routine,
        });
      }
    }
  }

  return out;
}

 

async function updateRoutineService({userId, body}) {
  const routineId = required(body.routine_id || body.id, "routine_id");
  const petId = required(body.pet_id, "pet_id");

  // We update the pet-specific assignment's schedule (next_activity) based on a provided start and interval
  const startInput = required(body.start_of_activity, "start_of_activity");
  const startStr = String(startInput).trim();
  const dtMatch = startStr.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})$/);
  if (!dtMatch) {
    const e = new Error("start_of_activity must be in YYYY-MM-DD HH:mm format"); e.status = 400; throw e;
  }
  const yyyy = parseInt(dtMatch[1], 10);
  const mm = parseInt(dtMatch[2], 10);
  const dd = parseInt(dtMatch[3], 10);
  const HH = parseInt(dtMatch[4], 10);
  const MM = parseInt(dtMatch[5], 10);
  if (mm < 1 || mm > 12 || dd < 1 || dd > 31 || HH < 0 || HH > 23 || MM < 0 || MM > 59) {
    const e = new Error("Invalid date/time values"); e.status = 400; throw e;
  }
  // Treat provided start as a naive local time; operate in UTC to avoid server TZ shifts
  function pad2(v) { return String(v).padStart(2, "0"); }
  function fmtUtc(dt) {
    return `${dt.getUTCFullYear()}-${pad2(dt.getUTCMonth() + 1)}-${pad2(dt.getUTCDate())} ${pad2(dt.getUTCHours())}:${pad2(dt.getUTCMinutes())}`;
  }
  const startUtc = new Date(Date.UTC(yyyy, mm - 1, dd, HH, MM, 0, 0));
  const startLocalStr = fmtUtc(startUtc); // keep the same string the user entered (no TZ offset)

  const everyNumRaw = required(body.perform_every_number, "perform_every_number");
  const n = Number(everyNumRaw);
  if (!Number.isInteger(n) || n <= 0) {
    const e = new Error("perform_every_number must be a positive integer"); e.status = 400; throw e;
  }
  const everyUnitRaw = required(body.perform_every_unit, "perform_every_unit");
  const unit = String(everyUnitRaw || "").toLowerCase();
  const allowedUnits = new Set(["hour", "hours", "day", "days", "week", "weeks", "month", "months"]);
  if (!allowedUnits.has(unit)) {
    const e = new Error("perform_every_unit must be one of hours|days|weeks|months"); e.status = 400; throw e;
  }

  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();

  // Find assignment row by routine_id + pet_id + user_id
  const raResp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "routineassignments!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const arows = raResp.data.values || [];
  if (arows.length <= 1) { const e = new Error("Assignment not found"); e.status = 404; throw e; }
  let foundIndex = -1;
  let assignmentId = "";
  for (let i = 1; i < arows.length; i++) {
    const cols = arows[i];
    if (String(cols[1]) === String(routineId) && String(cols[2]) === String(petId) && String(cols[3]) === String(userId)) {
      foundIndex = i; assignmentId = String(cols[0] || ""); break;
    }
  }
  if (foundIndex === -1) { const e = new Error("Assignment not found"); e.status = 404; throw e; }

  const nextActivity = addIntervalISO(startISO, n, unit);
  const rowNumber = foundIndex + 1;
  await sheets.spreadsheets.values.update({
    spreadsheetId,
    range: `routineassignments!G${rowNumber}:G${rowNumber}`,
    valueInputOption: "USER_ENTERED",
    requestBody: {values: [[nextActivity]]},
  });

  // Activity log
  const activityId = admin.firestore().collection("_ids").doc().id;
  const now = isoNow();
  const details = JSON.stringify({start_of_activity: startISO, perform_every_number: n, perform_every_unit: unit, next_activity: nextActivity});
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "activitylog!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      activityId,
      userId,
      "UPDATE_ROUTINE_ASSIGNMENT",
      "routineassignments",
      assignmentId,
      now,
      details,
    ]]},
  });

  return {assignment_id: assignmentId, next_activity: nextActivity, updated_at: now};
}
async function updatePetService({userId, body}) {
  const speciesOptions = ["Dog", "Cat", "Rabbit", "Bird", "Other"];
  const sexOptions = ["Male", "Female", "Unknown"];

  const petId = required(body.pet_id || body.id, "pet_id");
  const name = required(body.name, "name");
  const species = required(body.species, "species");
  const sex = required(body.sex, "sex");
  const breed = body.breed ? String(body.breed) : "";
  const dob = body.dob ? String(body.dob) : "";
  const weight = body.weight ? String(body.weight) : "";
  const color = body.color ? String(body.color) : "";

  const nameRegex = /^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$/;
  if (name.trim().length < 2 || name.trim().length > 50 || !nameRegex.test(name.trim())) {
    const e = new Error("Invalid name"); e.status = 400; throw e;
  }
  if (!speciesOptions.includes(species)) { const e = new Error("Invalid species"); e.status = 400; throw e; }
  if (!sexOptions.includes(sex)) { const e = new Error("Invalid sex"); e.status = 400; throw e; }
  if (breed.trim().length < 2 || breed.trim().length > 50 || !nameRegex.test(breed.trim())) { const e = new Error("Invalid breed"); e.status = 400; throw e; }
  const dobPattern = /^\d{4}-\d{2}-\d{2}$/;
  if (!dobPattern.test(dob.trim())) { const e = new Error("Invalid dob format"); e.status = 400; throw e; }
  const d = new Date(dob.trim());
  if (Number.isNaN(d.getTime())) { const e = new Error("Invalid dob date"); e.status = 400; throw e; }
  const today = new Date();
  const y = d.getUTCFullYear();
  if (d > today) { const e = new Error("DOB cannot be in the future"); e.status = 400; throw e; }
  if (y < 1900) { const e = new Error("DOB year must be >= 1900"); e.status = 400; throw e; }
  const fortyYearsMs = 40 * 365.25 * 24 * 3600 * 1000;
  if ((today - d) > fortyYearsMs) { const e = new Error("Unrealistic age"); e.status = 400; throw e; }

  if (!/^\d+$/.test(weight.trim()) || Number.parseInt(weight.trim(), 10) <= 0) { const e = new Error("Invalid weight"); e.status = 400; throw e; }
  const colorRegex = /^[A-Za-z ]+$/;
  if (!colorRegex.test(color.trim()) || color.trim().length === 0 || color.trim().length > 30) { const e = new Error("Invalid color"); e.status = 400; throw e; }
  const imageUrl = body.imageUrl ? String(body.imageUrl) : "";

  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();

  // Locate row by pet_id and user_id
  const r = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "pets!A1:K10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const rows = r.data.values || [];
  if (rows.length <= 1) { const e = new Error("Pet not found"); e.status = 404; throw e; }
  let foundIndex = -1;
  for (let i = 1; i < rows.length; i++) {
    const cols = rows[i];
    if (String(cols[0] || "") === String(petId) && (!cols[1] || String(cols[1]) === String(userId))) {
      foundIndex = i; break;
    }
  }
  if (foundIndex === -1) {
    const e = new Error("Pet not found"); e.status = 404; throw e;
  }

  const rowNumber = foundIndex + 1; // account for header
  // Update columns: C name, D imageUrl, E species, F sex, G breed, H dob, I weight, J color
  await sheets.spreadsheets.values.update({
    spreadsheetId,
    range: `pets!C${rowNumber}:J${rowNumber}`,
    valueInputOption: "USER_ENTERED",
    requestBody: {values: [[name, imageUrl, species, sex, breed, dob, weight, color]]},
  });

  // Activity log
  const activityId = admin.firestore().collection("_ids").doc().id;
  const now = isoNow();
  const details = JSON.stringify({name, species, sex, breed, dob, weight, color, imageUrl});
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "activitylog!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      activityId,
      userId,
      "UPDATE_PET",
      "pets",
      petId,
      now,
      details,
    ]]},
  });

  return {
    pet_id: petId,
    user_id: userId,
    name,
    species,
    sex,
    breed,
    dob,
    weight,
    color,
    imageUrl,
    updated_at: now,
  };
}
async function getPetNextEventsService({userId, petId, limit}) {
  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();
  const nowIso = isoNow();
  const max = Number.isFinite(Number(limit)) && Number(limit) > 0 ? Number(limit) : 3;

  // Preload routines and medications for name lookups
  const [routinesResp, medicationsResp] = await Promise.all([
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "routines!A1:Z10000",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "medications!A1:Z10000",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
  ]);
  const routineRows = routinesResp.data.values || [];
  const medicationRows = medicationsResp.data.values || [];
  const routineNameById = new Map(); // id -> name
  for (let i = 1; i < routineRows.length; i++) {
    const row = routineRows[i];
    routineNameById.set(String(row[0]), String(row[2] || ""));
  }
  const medicationNameById = new Map();
  for (let i = 1; i < medicationRows.length; i++) {
    const row = medicationRows[i];
    medicationNameById.set(String(row[0]), String(row[2] || ""));
  }

  // Get routine assignments upcoming
  const raResp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "routineassignments!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const ra = raResp.data.values || [];
  const routineEvents = [];
  for (let i = 1; i < ra.length; i++) {
    const cols = ra[i];
    const assignmentId = String(cols[0] || "");
    const routineId = String(cols[1] || "");
    const pid = String(cols[2] || "");
    const uid = String(cols[3] || "");
    const nextActivity = String(cols[6] || "");
    if (pid === String(petId) && uid === String(userId) && nextActivity) {
      if (new Date(nextActivity).getTime() >= new Date(nowIso).getTime()) {
        routineEvents.push({
          type: "routine",
          source_id: assignmentId,
          title: routineNameById.get(routineId) || "Routine",
          datetime: nextActivity,
        });
      }
    }
  }

  // Get medication assignments upcoming
  const maResp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "medicationassignments!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const ma = maResp.data.values || [];
  const medEvents = [];
  for (let i = 1; i < ma.length; i++) {
    const cols = ma[i];
    const assignmentId = String(cols[0] || "");
    const medicationId = String(cols[1] || "");
    const pid = String(cols[2] || "");
    const uid = String(cols[3] || "");
    const nextSupply = String(cols[6] || "");
    const isCompleted = String(cols[8] || "").toLowerCase() === "true";
    if (pid === String(petId) && uid === String(userId) && nextSupply && !isCompleted) {
      if (new Date(nextSupply).getTime() >= new Date(nowIso).getTime()) {
        medEvents.push({
          type: "medication",
          source_id: assignmentId,
          title: medicationNameById.get(medicationId) || "Medication",
          datetime: nextSupply,
        });
      }
    }
  }

  // Get upcoming health events
  const heResp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "healthevents!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const he = heResp.data.values || [];
  const healthEvents = [];
  for (let i = 1; i < he.length; i++) {
    const cols = he[i];
    const healthId = String(cols[0] || "");
    const pid = String(cols[1] || "");
    const uid = String(cols[2] || "");
    const eventName = String(cols[4] || "");
    const startOfActivity = String(cols[5] || "");
    if (pid === String(petId) && uid === String(userId) && startOfActivity) {
      if (new Date(startOfActivity).getTime() >= new Date(nowIso).getTime()) {
        healthEvents.push({
          type: "healthevent",
          source_id: healthId,
          title: eventName || "Health Event",
          datetime: startOfActivity,
        });
      }
    }
  }

  // Merge, sort, limit
  const merged = [...routineEvents, ...medEvents, ...healthEvents]
    .sort((a, b) => new Date(a.datetime).getTime() - new Date(b.datetime).getTime())
    .slice(0, max);

  return merged;
}

async function performMedicationService({ userId, body }) {
  const medicationId = required(body.medication_id || body.med_id || body.id, "medication_id");
  const petId = required(body.pet_id, "pet_id");

  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();

  // --- 1. Leer tabla medications ---
  const medsResp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "medications!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const mrows = medsResp.data.values || [];
  if (mrows.length <= 1) {
    const e = new Error("Medication not found");
    e.status = 404;
    throw e;
  }

  const medRow = mrows.find((cols, idx) => idx > 0 && String(cols[0]) === medicationId && String(cols[1]) === userId);
  if (!medRow) {
    const e = new Error("Medication not found");
    e.status = 404;
    throw e;
  }

  const everyNum = Number(medRow[4]);
  const everyUnit = String(medRow[5] || "");
  if (!Number.isFinite(everyNum) || !everyUnit) {
    const e = new Error("Medication interval missing");
    e.status = 400;
    throw e;
  }

  // --- 2. Leer assignments ---
  const maResp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "medicationassignments!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const arows = maResp.data.values || [];
  if (arows.length <= 1) {
    const e = new Error("Assignment not found");
    e.status = 404;
    throw e;
  }

  // columnas: A assignment_id, B medication_id, C pet_id, D user_id, E start_of_supply, F end_of_supply, G next_supply, H created_at, I is_completed
  let foundIndex = -1;
  let assignmentId = "";
  for (let i = 1; i < arows.length; i++) {
    const cols = arows[i];
    if (String(cols[1]) === medicationId && String(cols[2]) === petId && String(cols[3]) === userId) {
      foundIndex = i;
      assignmentId = String(cols[0] || "");
      break;
    }
  }

  if (foundIndex === -1) {
    const e = new Error("Assignment not found");
    e.status = 404;
    throw e;
  }

  // --- 3. Cálculos de fechas ---
  const row = arows[foundIndex];
  const startMsRaw = row[4];
  const endMsRaw = row[5];
  const nextMsRaw = row[6];

  const startMs = Number(startMsRaw);
  const endMs = Number(endMsRaw);
  const nFromG = Number(nextMsRaw);

  const hourMs = 60 * 60 * 1000;
  const dayMs = 24 * hourMs;
  const weekMs = 7 * dayMs;
  const unitLower = String(everyUnit || "").toLowerCase();
  const nInt = Number(everyNum) | 0;

  const stepMs =
    unitLower === "hour" || unitLower === "hours"
      ? nInt * hourMs
      : unitLower === "day" || unitLower === "days"
      ? nInt * dayMs
      : unitLower === "week" || unitLower === "weeks"
      ? nInt * weekMs
      : nInt * 30 * dayMs; // default month-based step

  const baseNextMs = Number.isFinite(nFromG)
    ? nFromG
    : Number.isFinite(startMs)
    ? startMs + stepMs
    : NaN;

  if (!Number.isFinite(baseNextMs)) {
    const e = new Error("Cannot derive next_supply");
    e.status = 400;
    throw e;
  }

  // --- 4. Calcular nuevas fechas ---
  const newStartMs = baseNextMs;
  const newNextMs = baseNextMs + stepMs;

  // --- 5. Determinar si el tratamiento ya terminó ---
  let isCompleted = false;
  if (Number.isFinite(endMs) && newStartMs >= endMs) {
    isCompleted = true;
  }

  // --- 6. Actualizar fila en Sheets ---
  const rowNumber = foundIndex + 1;
  await sheets.spreadsheets.values.update({
    spreadsheetId,
    range: `medicationassignments!E${rowNumber}:I${rowNumber}`,
    valueInputOption: "RAW",
    requestBody: {
      values: [[newStartMs, endMsRaw || "", newNextMs, row[7] || "", isCompleted]],
    },
  });

  // --- 7. Registrar actividad ---
  const activityId = admin.firestore().collection("_ids").doc().id;
  const now = isoNow();
  const details = JSON.stringify({
    start_of_supply: newStartMs,
    next_supply: newNextMs,
    is_completed: isCompleted,
  });

  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "activitylog!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {
      values: [
        [activityId, userId, "PERFORM_MEDICATION", "medicationassignments", assignmentId, now, details],
      ],
    },
  });

  // --- 8. Retornar resultado actualizado ---
  return {
    assignment_id: assignmentId,
    start_of_supply: newStartMs,
    next_supply: newNextMs,
    is_completed: isCompleted,
    updated_at: now,
  };
}

async function createMedicationService({ userId, body }) {
  const rawName = required(body.medication_name || body.title || body.name, "medication_name");
  const medicationName = String(rawName).trim();
  const nameRegex = /^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$/;
  if (medicationName.length < 2 || medicationName.length > 50 || !nameRegex.test(medicationName)) {
    const e = new Error("Invalid medication_name");
    e.status = 400;
    throw e;
  }

  // 🔹 Prefer client-provided epoch (local user time). Fallback to parsing string.
  let startEpochMs = 0;
  let startStr = "";
  const candidateEpoch = Number(body.start_epoch_ms || 0);
  if (Number.isFinite(candidateEpoch) && candidateEpoch > 0) {
    startEpochMs = Math.trunc(candidateEpoch);
    startStr = String(body.start_of_supply || body.start_of_activity || "");
  } else {
    const startInput = required(body.start_of_supply || body.start_of_activity, "start_of_supply");
    startStr = String(startInput).trim();
    const dtMatch = startStr.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})$/);
    if (!dtMatch) {
      const e = new Error("start_of_supply must be in YYYY-MM-DD HH:mm format");
      e.status = 400;
      throw e;
    }
    const yyyy = parseInt(dtMatch[1], 10);
    const mm = parseInt(dtMatch[2], 10);
    const dd = parseInt(dtMatch[3], 10);
    const HH = parseInt(dtMatch[4], 10);
    const MM = parseInt(dtMatch[5], 10);
    const startDate = new Date(yyyy, mm - 1, dd, HH, MM, 0, 0);
    startEpochMs = startDate.getTime();
  }

  // 🔹 Frequency validation
  const everyNumRaw = required(body.perform_every_number, "perform_every_number");
  const n = Number(everyNumRaw);
  if (!Number.isInteger(n) || n <= 0) {
    const e = new Error("perform_every_number must be a positive integer");
    e.status = 400;
    throw e;
  }

  const everyUnitRaw = required(body.perform_every_unit, "perform_every_unit");
  const unit = String(everyUnitRaw || "").toLowerCase();
  const allowedUnits = new Set(["hour", "hours", "day", "days", "week", "weeks", "month", "months"]);
  if (!allowedUnits.has(unit)) {
    const e = new Error("perform_every_unit must be one of hours|days|weeks|months");
    e.status = 400;
    throw e;
  }

  // 🔹 Dose validation
  const doseValueRaw = required(body.dose_number || body.dose_value, "dose_number");
  const doseNumber = Number(String(doseValueRaw).trim());
  if (!(doseNumber > 0)) {
    const e = new Error("dose_number must be > 0");
    e.status = 400;
    throw e;
  }
  const doseUnit = required(body.dose_unit, "dose_unit");

  // 🔹 Interval iteration (calendar-aware in local time to handle DST/months)
  const totalDoses = Number(required(body.total_doses, "total_doses"));
  const addIntervalLocalEpoch = (baseEpochMs, number, unitStr) => {
    const d = new Date(baseEpochMs);
    const u = String(unitStr || "").toLowerCase();
    switch (u) {
      case "hour":
      case "hours":
        d.setHours(d.getHours() + number);
        break;
      case "day":
      case "days":
        d.setDate(d.getDate() + number);
        break;
      case "week":
      case "weeks":
        d.setDate(d.getDate() + 7 * number);
        break;
      case "month":
      case "months":
        d.setMonth(d.getMonth() + number);
        break;
      default:
        throw Object.assign(new Error("perform_every_unit must be one of hours|days|weeks|months"), {status: 400});
    }
    return d.getTime();
  };
  // Próxima dosis = una iteración desde el inicio
  const nextDoseEpoch = addIntervalLocalEpoch(startEpochMs, n, unit);
  // Fin de suministro: iterar 'totalDoses' veces desde el inicio
  let endEpoch = startEpochMs;
  for (let i = 0; i < totalDoses; i++) {
    endEpoch = addIntervalLocalEpoch(endEpoch, n, unit);
  }

  logger.info("createMedication: computed times", {
    user_id: userId,
    medication_name: medicationName,
    start_input: startStr,
    start_epoch_ms: startEpochMs,
    start_local: new Date(startEpochMs).toString(),
    n,
    unit,
    next_epoch_ms: nextDoseEpoch,
    next_local: new Date(nextDoseEpoch).toString(),
    total_doses: totalDoses,
    end_epoch_ms: endEpoch,
    end_local: new Date(endEpoch).toString(),
  });

  const spreadsheetId = ensureSpreadsheetId();
  const now = Date.now();
  const medicationId = admin.firestore().collection("_ids").doc().id;

  const sheets = await getSheetsClient();

  // 🔹 Medications sheet
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "medications!A1",
    valueInputOption: "RAW",
    insertDataOption: "INSERT_ROWS",
    requestBody: {
      values: [[
        medicationId,
        userId,
        medicationName,
        startEpochMs,
        n,
        unit,
        doseNumber,
        doseUnit,
        totalDoses,
        now
      ]]
    },
  });

  // 🔹 Assignments (current schema: E start_of_supply, F end_of_supply, G next_supply, H created_at, I is_completed)
  const petIds = Array.isArray(body.assign_to_pets) ? body.assign_to_pets.map(String) : [];
  if (petIds.length > 0) {
    const rows = petIds.map(pid => [
      admin.firestore().collection("_ids").doc().id, // A assignment_id
      medicationId,                                   // B medication_id
      pid,                                            // C pet_id
      userId,                                         // D user_id
      startEpochMs,                                   // E start_of_supply
      endEpoch,                                       // F end_of_supply
      nextDoseEpoch,                                  // G next_supply
      now,                                            // H created_at
      "false",                                       // I is_completed
    ]);
    await sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "medicationassignments!A1",
      valueInputOption: "RAW",
      insertDataOption: "INSERT_ROWS",
      requestBody: { values: rows },
    });
  }

  return {
    medication: {
      medication_id: medicationId,
      user_id: userId,
      medication_name: medicationName,
      start_of_supply: startEpochMs,
      perform_every_number: n,
      perform_every_unit: unit,
      dose_number: doseNumber,
      dose_unit: doseUnit,
      total_doses: totalDoses,
      next_dose: nextDoseEpoch,
      end_of_supply: endEpoch,
      created_at: now,
    },
  };
}


function ensureSpreadsheetId() {
  const spreadsheetId = process.env.SPREADSHEET_ID;
  if (!spreadsheetId) {
    const e = new Error("Missing SPREADSHEET_ID env var");
    e.status = 500;
    throw e;
  }
  return spreadsheetId;
}

function required(v, name) {
  if (v === undefined || v === null || String(v).trim() === "") {
    const e = new Error(`${name} is required`);
    e.status = 400;
    throw e;
  }
  return String(v).trim();
}

function isoNow() {
  return new Date().toISOString();
}

function addIntervalISO(baseIso, number, unit) {
  // Helpers for local parsing/formatting
  const pad = (x) => String(x).padStart(2, "0");
  const formatLocal = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  const parseLocal = (s) => {
    if (typeof s !== "string") return new Date(NaN);
    const m = s.trim().match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/);
    if (!m) return new Date(NaN);
    const [_, yyyy, mm, dd, HH, MM] = m;
    return new Date(Number(yyyy), Number(mm) - 1, Number(dd), Number(HH), Number(MM));
  };

  // Fecha base como local-naive
  const dt = parseLocal(baseIso);
  if (Number.isNaN(dt.getTime())) {
    const err = new Error(`Invalid start_of_activity (expected YYYY-MM-DD HH:mm): ${baseIso}`);
    err.status = 400;
    throw err;
  }

  const n = Number(number);
  if (!Number.isFinite(n)) {
    const err = new Error("perform_every_number must be numeric");
    err.status = 400;
    throw err;
  }

  const unitStr = String(unit || "").toLowerCase();
  switch (unitStr) {
    case "hour":
    case "hours":
      dt.setHours(dt.getHours() + n); break;
    case "day":
    case "days":
      dt.setDate(dt.getDate() + n); break;
    case "week":
    case "weeks":
      dt.setDate(dt.getDate() + 7 * n); break;
    case "month":
    case "months":
      dt.setMonth(dt.getMonth() + n); break;
    default:
      const err2 = new Error("perform_every_unit must be one of hours|days|weeks|months");
      err2.status = 400;
      throw err2;
  }

  const out = formatLocal(dt);
  logger.info(`[addIntervalISO] base='${baseIso}' -> '${out}' (${unitStr} +${n})`);
  return out;
}

function calculateNextAndLast(baseIso, n, unit, clientNowStr, startEpochMs, clientNowEpochMs) {
  const pad = (x) => String(x).padStart(2, "0");
  const formatLocal = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  const parseLocal = (s) => {
    if (typeof s !== "string") return new Date(NaN);
    const m = s.trim().match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/);
    if (!m) return new Date(NaN);
    const [_, yyyy, mm, dd, HH, MM] = m;
    // ⚡ Importante: construimos una fecha "naive local", sin zonas horarias
    return new Date(Number(yyyy), Number(mm) - 1, Number(dd), Number(HH), Number(MM));
  };
  // Si recibimos epoch desde el cliente, usemos ese camino (evita drift por TZ/DST)
  const hasStartEpoch = Number.isFinite(Number(startEpochMs)) && Number(startEpochMs) > 0;
  const hasClientNowEpoch = Number.isFinite(Number(clientNowEpochMs)) && Number(clientNowEpochMs) > 0;

  if (hasStartEpoch) {
    const baseMs = Number(startEpochMs);
    // Derivar nowMs para comparación
    const nowMs = hasClientNowEpoch ? Number(clientNowEpochMs) : Date.now();
    // Intervalos en ms (mes = 30 días como en lógica actual)
    const hourMs = 60 * 60 * 1000;
    const dayMs = 24 * hourMs;
    const weekMs = 7 * dayMs;
    const unitLower = String(unit || "").toLowerCase();
    const nInt = Number(n) | 0;
    const stepMs = (unitLower === "hour" || unitLower === "hours") ? nInt * hourMs
                  : (unitLower === "day" || unitLower === "days") ? nInt * dayMs
                  : (unitLower === "week" || unitLower === "weeks") ? nInt * weekMs
                  : /* month(s) */ nInt * 30 * dayMs;
    if (stepMs <= 0) {
      return { last_performed_at: baseMs, next_activity: baseMs };
    }
    if (baseMs > nowMs) {
      return { last_performed_at: "", next_activity: baseMs };
    }
    let next = baseMs;
    let iter = 0;
    while (next <= nowMs) {
      next += stepMs;
      iter++;
      if (iter > 100000) break;
    }
    return { last_performed_at: baseMs, next_activity: next };
  }

  // Offset estimado a partir de client_now vs servidor (fallback si no hay epoch del cliente)
  let clientOffsetMs = 0;
  if (clientNowStr && String(clientNowStr).trim()) {
    const m = String(clientNowStr).trim().match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})$/);
    if (m) {
      const yyyy = Number(m[1]);
      const mm = Number(m[2]);
      const dd = Number(m[3]);
      const HH = Number(m[4]);
      const MM = Number(m[5]);
      const clientNowUtcByLocal = Date.UTC(yyyy, mm - 1, dd, HH, MM, 0, 0);
      const serverUtcNow = Date.now();
      clientOffsetMs = clientNowUtcByLocal - serverUtcNow;
    }
  }
  const toUtcMsFromLocalString = (s) => {
    const m = String(s).trim().match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})$/);
    if (!m) return NaN;
    const yyyy = Number(m[1]);
    const mm = Number(m[2]);
    const dd = Number(m[3]);
    const HH = Number(m[4]);
    const MM = Number(m[5]);
    // UTC = local - offset
    return Date.UTC(yyyy, mm - 1, dd, HH, MM, 0, 0) - clientOffsetMs;
  };

  // 📅 Parseamos la fecha base del usuario como local
  const start = parseLocal(baseIso);
  if (Number.isNaN(start.getTime())) {
    throw new Error(`Invalid baseIso: ${baseIso}`);
  }

  // ⚙️ "now" local basado en clientNowStr si viene; si no, usamos hora local del servidor
  let localNow;
  if (clientNowStr && String(clientNowStr).trim()) {
    localNow = parseLocal(String(clientNowStr).trim());
  } else {
    const now = new Date();
    localNow = new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours(), now.getMinutes());
  }

  logger.info(`[calcNext] Comparando en hora local del usuario`);
  logger.info(`[calcNext] start=${formatLocal(start)}, now=${formatLocal(localNow)}`);

  // 📍 Si la fecha ingresada es futura
  if (start > localNow) {
    logger.info(`[calcNext] Fecha futura detectada — next=${formatLocal(start)}, last=(ninguno)`);
    const nextLocalStr = formatLocal(start);
    return {
      // last vacío (sin ejecución aún), next en epoch UTC (ms) derivado de la hora "naive local"
      last_performed_at: "",
      next_activity: toUtcMsFromLocalString(nextLocalStr),
    };
  }

  // 📍 Si la fecha ingresada es pasada
  let next = new Date(start.getTime());
  let iterations = 0;

  while (next <= localNow) {
    iterations++;
    const nextStr = addIntervalISO(formatLocal(next), n, unit);
    const newNext = parseLocal(nextStr);
    const condition = newNext > localNow;
    logger.info(`[calcNext] Iteración ${iterations} — comparando ${formatLocal(newNext)} > ${formatLocal(localNow)} = ${condition}`);

    next = newNext;
    if (iterations > 10000) throw new Error("Loop infinito detectado");
  }

  logger.info(`[calcNext] Resultado final — last=${formatLocal(start)}, next=${formatLocal(next)}`);

  const lastLocalStr = formatLocal(start);
  const nextLocalStr = formatLocal(next);
  return {
    // ambos en epoch UTC (ms) derivados de las cadenas "naive local"
    last_performed_at: toUtcMsFromLocalString(lastLocalStr),
    next_activity: toUtcMsFromLocalString(nextLocalStr),
  };
}

async function performRoutineService({userId, body}) {
  const routineId = required(body.routine_id || body.id, "routine_id");
  const petId = required(body.pet_id, "pet_id");

  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();

  // Read routines to get interval by routine_id + user_id
  const routinesResp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "routines!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const rrows = routinesResp.data.values || [];
  if (rrows.length <= 1) { const e = new Error("Routine not found"); e.status = 404; throw e; }
  // columns: A routine_id, B user_id, C routine_name, D start_of_activity, E perform_every_number, F perform_every_unit
  const routineRow = rrows.find((cols, idx) => idx > 0 && String(cols[0]) === routineId && String(cols[1]) === userId);
  if (!routineRow) { const e = new Error("Routine not found"); e.status = 404; throw e; }
  const everyNum = Number(routineRow[4]);
  const everyUnit = String(routineRow[5] || "");
  if (!Number.isFinite(everyNum) || !everyUnit) { const e = new Error("Routine interval missing"); e.status = 400; throw e; }

  // Read routineassignments to locate assignment row by routine_id + pet_id + user_id
  const raResp = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "routineassignments!A1:Z10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const arows = raResp.data.values || [];
  if (arows.length <= 1) { const e = new Error("Assignment not found"); e.status = 404; throw e; }
  // columns: A assignment_id, B routine_id, C pet_id, D user_id, E assigned_at, F last_performed_at, G next_activity
  let foundIndex = -1;
  let assignmentId = "";
  for (let i = 1; i < arows.length; i++) {
    const cols = arows[i];
    if (String(cols[1]) === routineId && String(cols[2]) === petId && String(cols[3]) === userId) {
      foundIndex = i; assignmentId = String(cols[0] || ""); break;
    }
  }
  if (foundIndex === -1) { const e = new Error("Assignment not found"); e.status = 404; throw e; }

  const row = arows[foundIndex];
  const currentNextRaw = row[6];
  const currentNextMs = Number(currentNextRaw);
  if (!Number.isFinite(currentNextMs)) { const e = new Error("Current next_activity is invalid"); e.status = 400; throw e; }

  // compute stepMs from interval
  const hourMs = 60 * 60 * 1000;
  const dayMs = 24 * hourMs;
  const weekMs = 7 * dayMs;
  const unitLower = everyUnit.toLowerCase();
  const nInt = Number(everyNum) | 0;
  const stepMs = (unitLower === "hour" || unitLower === "hours") ? nInt * hourMs
                : (unitLower === "day" || unitLower === "days") ? nInt * dayMs
                : (unitLower === "week" || unitLower === "weeks") ? nInt * weekMs
                : /* month(s) */ nInt * 30 * dayMs;

  const newLastMs = currentNextMs;
  const newNextMs = currentNextMs + stepMs;

  // Update last_performed_at (F) and next_activity (G) in epoch ms
  const rowNumber = foundIndex + 1; // account for header
  await sheets.spreadsheets.values.update({
    spreadsheetId,
    range: `routineassignments!F${rowNumber}:G${rowNumber}`,
    valueInputOption: "RAW",
    requestBody: {values: [[newLastMs, newNextMs]]},
  });

  // Log activity
  const activityId = admin.firestore().collection("_ids").doc().id;
  const now = isoNow();
  const details = JSON.stringify({ last_performed_at: newLastMs, next_activity: newNextMs });
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "activitylog!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      activityId,
      userId,
      "PERFORM_ROUTINE",
      "routineassignments",
      assignmentId,
      now,
      details,
    ]]},
  });

  return {assignment_id: assignmentId, last_performed_at: newLastMs, next_activity: newNextMs, updated_at: now};
}

async function getPetsService({userId}) {
  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();
  const r = await sheets.spreadsheets.values.get({
    spreadsheetId,
    range: "pets!A1:K10000",
    valueRenderOption: "UNFORMATTED_VALUE",
    dateTimeRenderOption: "FORMATTED_STRING",
  });
  const rows = r.data.values || [];
  if (rows.length <= 1) return [];
  const allPets = rows.slice(1).map(cols => ({
    pet_id: cols[0] || "",
    user_id: cols[1] || "",
    name: cols[2] || "",
    imageUrl: cols[3] || "",
    species: cols[4] || "",
    sex: cols[5] || "",
    breed: cols[6] || "",
    date_of_birth: cols[7] || "",
    weight_kg: cols[8] || "",
    color: cols[9] || "",
    created_at: cols[10] || "",
  }));
  // Filtrar por userId
  const out = allPets.filter(pet => pet.user_id === userId);
  return out;
}

async function createPetService({userId, body}) {
  const speciesOptions = ["Dog", "Cat", "Rabbit", "Bird", "Other"];
  const sexOptions = ["Male", "Female", "Unknown"];

  const name = required(body.name, "name");
  const species = required(body.species, "species");
  const sex = required(body.sex, "sex");
  const breed = body.breed ? String(body.breed) : "";
  const dob = body.dob ? String(body.dob) : "";
  const weight = body.weight ? String(body.weight) : "";
  const color = body.color ? String(body.color) : "";

  const nameRegex = /^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$/;
  if (name.trim().length < 2 || name.trim().length > 50 || !nameRegex.test(name.trim())) {
    const e = new Error("Invalid name"); e.status = 400; throw e;
  }
  if (!speciesOptions.includes(species)) {
    const e = new Error("Invalid species"); e.status = 400; throw e;
  }
  if (!sexOptions.includes(sex)) {
    const e = new Error("Invalid sex"); e.status = 400; throw e;
  }
  if (breed.trim().length < 2 || breed.trim().length > 50 || !nameRegex.test(breed.trim())) {
    const e = new Error("Invalid breed"); e.status = 400; throw e;
  }
  const dobPattern = /^\d{4}-\d{2}-\d{2}$/;
  if (!dobPattern.test(dob.trim())) {
    const e = new Error("Invalid dob format"); e.status = 400; throw e;
  }
  const d = new Date(dob.trim());
  if (Number.isNaN(d.getTime())) {
    const e = new Error("Invalid dob date"); e.status = 400; throw e;
  }
  const today = new Date();
  const y = d.getUTCFullYear();
  if (d > today) {
    const e = new Error("DOB cannot be in the future"); e.status = 400; throw e;
  }
  if (y < 1900) {
    const e = new Error("DOB year must be >= 1900"); e.status = 400; throw e;
  }
  const fortyYearsMs = 40 * 365.25 * 24 * 3600 * 1000;
  if ((today - d) > fortyYearsMs) {
    const e = new Error("Unrealistic age"); e.status = 400; throw e;
  }

  if (!/^\d+$/.test(weight.trim()) || Number.parseInt(weight.trim(), 10) <= 0) {
    const e = new Error("Invalid weight"); e.status = 400; throw e;
  }

  const colorRegex = /^[A-Za-z ]+$/;
  if (!colorRegex.test(color.trim()) || color.trim().length === 0 || color.trim().length > 30) {
    const e = new Error("Invalid color"); e.status = 400; throw e;
  }
  const imageUrl = body.imageUrl ? String(body.imageUrl) : "";

  const spreadsheetId = ensureSpreadsheetId();
  const petId = admin.firestore().collection("_ids").doc().id;
  const now = isoNow();

  const values = [[
    petId,
    userId,
    name,
    imageUrl,
    species,
    sex,
    breed,
    dob,
    weight,
    color,
    now,
  ]];

  const sheets = await getSheetsClient();
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "pets!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values},
  });

  // Log activity for pet creation
  const activityId = admin.firestore().collection("_ids").doc().id;
  const details = JSON.stringify({
    name,
    species,
    sex,
    breed,
    dob,
    weight,
    color,
    imageUrl,
  });
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "activitylog!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      activityId,
      userId,
      "CREATE_PET",
      "pets",
      petId,
      now,
      details,
    ]]},
  });

  return {
    pet_id: petId,
    user_id: userId,
    name,
    species,
    sex,
    breed,
    dob,
    weight,
    color,
    imageUrl,
    created_at: now,
    updated_at: now,
  };
}

async function createRoutineService({userId, body}) {
  const rawRoutineName = required(body.routine_name || body.title || body.name, "routine_name");
  const routineName = String(rawRoutineName).trim();
  const nameRegex = /^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$/;
  if (routineName.length < 2 || routineName.length > 50 || !nameRegex.test(routineName)) {
    const e = new Error("Invalid routine_name"); e.status = 400; throw e;
  }

  const startInput = required(body.start_of_activity, "start_of_activity");
  const startStr = String(startInput).trim();
  // Validate the textual format (still required as fallback/backwards compat)
  const dtMatch = startStr.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})$/);
  if (!dtMatch) {
    const e = new Error("start_of_activity must be in YYYY-MM-DD HH:mm format"); e.status = 400; throw e;
  }
  const yyyy = parseInt(dtMatch[1], 10);
  const mm = parseInt(dtMatch[2], 10);
  const dd = parseInt(dtMatch[3], 10);
  const HH = parseInt(dtMatch[4], 10);
  const MM = parseInt(dtMatch[5], 10);
  if (mm < 1 || mm > 12 || dd < 1 || dd > 31 || HH < 0 || HH > 23 || MM < 0 || MM > 59) {
    const e = new Error("Invalid date/time values"); e.status = 400; throw e;
  }

  // ---------- PRIORIDAD: usar epoch que mande el cliente -------------
  const startEpochMsFromClient = Number(body.start_epoch_ms || 0);
  // clientNowEpochMs es opcional, si viene lo usamos para comparaciones
  const clientNowEpochMs = Number(body.client_now_epoch_ms || 0);

  // Helper: formateo local sin zona (YYYY-MM-DD HH:mm)
  const pad = (x) => String(x).padStart(2, "0");
  const formatLocal = (d) =>
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;

  let startEpochMs;
  let startLocalStr;
  let startISO;

  if (Number.isFinite(startEpochMsFromClient) && startEpochMsFromClient > 0) {
    // Si el cliente envía epoch, lo usamos como fuente de la verdad
    startEpochMs = Math.floor(startEpochMsFromClient);
    const d = new Date(startEpochMs);
    startLocalStr = formatLocal(d);
    startISO = d.toISOString();
    logger.info(`[createRoutineService] Using client epoch start_epoch_ms=${startEpochMs} -> local='${startLocalStr}' iso='${startISO}'`);
  } else {
    // Si no viene epoch, parseamos el startStr **como hora local del cliente** (naive)
    const dLocal = new Date(yyyy, mm - 1, dd, HH, MM, 0, 0); // local-naive
    startEpochMs = dLocal.getTime();
    startLocalStr = formatLocal(dLocal);
    startISO = new Date(startEpochMs).toISOString();
    logger.info(`[createRoutineService] No client epoch. Parsed startStr='${startStr}' -> epoch=${startEpochMs} local='${startLocalStr}' iso='${startISO}'`);
  }

  // Validación de intervalos
  const everyNumRaw = required(body.perform_every_number, "perform_every_number");
  const n = Number(everyNumRaw);
  if (!Number.isInteger(n) || n <= 0) {
    const e = new Error("perform_every_number must be a positive integer"); e.status = 400; throw e;
  }
  const everyUnitRaw = required(body.perform_every_unit, "perform_every_unit");
  const unit = String(everyUnitRaw || "").toLowerCase();
  const allowedUnits = new Set(["hour", "hours", "day", "days", "week", "weeks", "month", "months"]);
  if (!allowedUnits.has(unit)) {
    const e = new Error("perform_every_unit must be one of hours|days|weeks|months"); e.status = 400; throw e;
  }

  const petIds = Array.isArray(body.assign_to_pets) ? body.assign_to_pets.map(String) : [];

  const spreadsheetId = ensureSpreadsheetId();
  const now = isoNow();
  const routineId = admin.firestore().collection("_ids").doc().id;
  const clientNow = body.client_now ? String(body.client_now).trim() : "";

  // Llamamos a calculateNextAndLast: le pasamos la representación local (startLocalStr) Y el epoch
  // Asumimos que calculateNextAndLast prioriza el epoch si se le pasa (si no: ajusta esa función también)
  const { last_performed_at, next_activity } = calculateNextAndLast(
    startLocalStr, // texto local (fallback)
    n,
    unit,
    clientNow,           // clientNow string fallback (opcional)
    startEpochMs,        // start epoch (preferido)
    clientNowEpochMs     // client now epoch (opcional)
  );

  // LOG final de chequeo
  logger.info(`[createRoutineService] final: routineId=${routineId}, startEpochMs=${startEpochMs}, startLocal='${startLocalStr}', next='${next_activity}', last='${last_performed_at}', clientNowEpochMs=${clientNowEpochMs}`);

  const sheets = await getSheetsClient();

  // Guardamos EN RUTINES: usamos el ISO (para compatibilidad histórica),
  // pero dejamos la assignment con los valores locales (last/next) tal como antes.
  // Si quieres que también routines.D sea local string en vez de ISO, cambia String(startISO) por startLocalStr.
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "routines!A1",
    valueInputOption: "RAW",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      routineId,
      userId,
      routineName,
      String(startISO), // mantengo ISO para backwards compat; si prefieres local: use startLocalStr
      String(n),
      String(unit),
      now,
    ]]},
  });

  const assignments = [];
  for (const pid of petIds) {
    const assignmentId = admin.firestore().collection("_ids").doc().id;
    assignments.push({
      assignment_id: assignmentId,
      user_id: userId,
      pet_id: pid,
      routine_id: routineId,
      last_performed_at: last_performed_at,
      next_activity: next_activity,
      assigned_at: now,
    });
  }
  if (assignments.length > 0) {
    const values = assignments.map(a => [
      a.assignment_id,
      a.routine_id,
      a.pet_id,
      a.user_id,
      a.assigned_at,
      a.last_performed_at,
      a.next_activity,
    ]);
    await sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "routineassignments!A1",
      valueInputOption: "RAW",
      insertDataOption: "INSERT_ROWS",
      requestBody: {values},
    });
  }

  const activityId = admin.firestore().collection("_ids").doc().id;
  const details = JSON.stringify({
    routine_name: routineName,
    start_of_activity: startISO,
    perform_every_number: n,
    perform_every_unit: unit,
    assign_to_pets: petIds,
  });
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "activitylog!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      activityId,
      userId,
      "CREATE_ROUTINE",
      "routines",
      routineId,
      now,
      details,
    ]]},
  });

  logger.info("Medication time debug", {
  input: startStr,
  parsed_epoch: startEpochMs,
  local_str: new Date(startEpochMs).toString(),
  iso_str: new Date(startEpochMs).toISOString(),
});


  return {
    routine: {
      routine_id: routineId,
      user_id: userId,
      routine_name: routineName,
      perform_every_number: Number(n),
      perform_every_unit: String(unit),
      start_of_activity: String(startISO),
      next_activity: next_activity,
      created_at: now,
    },
    assignments,
  };
}

async function deleteMedicationService({ userId, body }) {
  const medicationId = required(body.medication_id || body.id, "medication_id");
  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();
  // Load medications and assignments
  const [mResp, aResp] = await Promise.all([
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "medications!A1:Z500",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
    sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "medicationassignments!A1:Z500",
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    }),
  ]);
  const mrows = mResp.data.values || [];
  const arows = aResp.data.values || [];
  if (mrows.length === 0) { const e = new Error("medications sheet missing"); e.status = 500; throw e; }
  if (arows.length === 0) { const e = new Error("medicationassignments sheet missing"); e.status = 500; throw e; }

  const clean = (s) => String(s || "").trim();
  const mHeader = mrows[0];
  const aHeader = arows[0];
  const mFiltered = [mHeader, ...mrows.slice(1).filter(cols => clean(cols[0]) !== clean(medicationId))];
  const aFiltered = [aHeader, ...arows.slice(1).filter(cols => clean(cols[1]) !== clean(medicationId))];

  await Promise.all([
    sheets.spreadsheets.values.clear({ spreadsheetId, range: "medications!A2:Z500" }),
    sheets.spreadsheets.values.clear({ spreadsheetId, range: "medicationassignments!A2:Z500" }),
  ]);

  const activityId = admin.firestore().collection("_ids").doc().id;
  const now = isoNow();
  const details = JSON.stringify({ medication_id: medicationId });

  const [updateMedsResp, updateAssignResp, appendLogResp] = await Promise.all([
    sheets.spreadsheets.values.update({
      spreadsheetId,
      range: "medications!A1",
      valueInputOption: "USER_ENTERED",
      requestBody: { values: mFiltered },
    }),
    sheets.spreadsheets.values.update({
      spreadsheetId,
      range: "medicationassignments!A1",
      valueInputOption: "USER_ENTERED",
      requestBody: { values: aFiltered },
    }),
    sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "activitylog!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: { values: [[activityId, userId, "DELETE_MEDICATION", "medications", medicationId, now, details]] },
    }),
  ]);

  return { medication_id: medicationId, deleted: (mrows.length - mFiltered.length) > 0 || (arows.length - aFiltered.length) > 0, deleted_at: now };
}

module.exports = {
  getPetsService,
  createPetService,
  getRoutinesByPetService,
  getMedicationsByPetService,
  updateRoutineService,
  createRoutineService,
  createMedicationService,
  performMedicationService,
  performRoutineService,
  getPetNextEventsService,
  createOrUpdateUserService,
  deleteRoutineService,
  deleteMedicationService,
};