const admin = require("firebase-admin");
const {google} = require("googleapis");

const SCOPES = ["https://www.googleapis.com/auth/spreadsheets"];

async function getSheetsClient() {
  const auth = await google.auth.getClient({scopes: SCOPES});
  return google.sheets({version: "v4", auth});
}

async function createMedicationService({userId, body}) {
  const rawName = required(body.medication_name || body.title || body.name, "medication_name");
  const medicationName = String(rawName).trim();
  const nameRegex = /^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$/;
  if (medicationName.length < 2 || medicationName.length > 50 || !nameRegex.test(medicationName)) {
    const e = new Error("Invalid medication_name"); e.status = 400; throw e;
  }

  const startInput = required(body.start_of_supply || body.start_of_activity, "start_of_supply");
  const startStr = String(startInput).trim();
  const dtMatch = startStr.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})$/);
  if (!dtMatch) {
    const e = new Error("start_of_supply must be in YYYY-MM-DD HH:mm format"); e.status = 400; throw e;
  }
  const yyyy = parseInt(dtMatch[1], 10);
  const mm = parseInt(dtMatch[2], 10);
  const dd = parseInt(dtMatch[3], 10);
  const HH = parseInt(dtMatch[4], 10);
  const MM = parseInt(dtMatch[5], 10);
  if (mm < 1 || mm > 12 || dd < 1 || dd > 31 || HH < 0 || HH > 23 || MM < 0 || MM > 59) {
    const e = new Error("Invalid date/time values"); e.status = 400; throw e;
  }
  const startISO = new Date(Date.UTC(yyyy, mm - 1, dd, HH, MM, 0, 0)).toISOString();

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

  const doseValueRaw = required(body.dose_number || body.dose_value, "dose_number");
  const doseStr = String(doseValueRaw).trim();
  if (!/^\d+(\.\d{1,2})?$/.test(doseStr)) {
    const e = new Error("dose_number must be > 0 with up to 2 decimals");
    e.status = 400;
    throw e;
  }
  const doseNumber = Number(doseStr);
  if (!(doseNumber > 0)) {
    const e = new Error("dose_number must be > 0");
    e.status = 400;
    throw e;
  }
  const doseUnit = required(body.dose_unit, "dose_unit");
  const allowedDoseUnits = new Set(["mg", "ml", "g", "drops", "tablet"]);
  if (!allowedDoseUnits.has(String(doseUnit))) {
    const e = new Error("dose_unit must be one of mg|ml|g|drops|tablet"); e.status = 400; throw e;
  }

  const spreadsheetId = ensureSpreadsheetId();
  const now = isoNow();
  const medicationId = admin.firestore().collection("_ids").doc().id;
  const nextSupply = addIntervalISO(startISO, n, unit);

  const sheets = await getSheetsClient();

  // medications sheet: medication_id, user_id, medication_name, start_of_supply, perform_every_number, perform_every_unit, dose_number, dose_unit, end_of_supply, next_supply, created_at
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "medications!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      medicationId,
      userId,
      medicationName,
      startISO,
      String(n),
      String(unit),
      String(doseNumber),
      String(doseUnit),
      "",
      nextSupply,
      now,
    ]]},
  });

  // medicationassignments if assign_to_pets provided
  const petIds = Array.isArray(body.assign_to_pets) ? body.assign_to_pets.map(String) : [];
  if (petIds.length > 0) {
    const rows = petIds.map(pid => [
      admin.firestore().collection("_ids").doc().id,
      medicationId,
      pid,
      userId,
      now,
    ]);
    await sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "medicationassignments!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: {values: rows},
    });
  }

  // Activity log
  const activityId = admin.firestore().collection("_ids").doc().id;
  const details = JSON.stringify({
    medication_name: medicationName,
    start_of_supply: startISO,
    perform_every_number: n,
    perform_every_unit: unit,
    dose_number: doseNumber,
    dose_unit: doseUnit,
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
      "CREATE_MEDICATION",
      "medications",
      medicationId,
      now,
      details,
    ]]},
  });

  return {
    medication: {
      medication_id: medicationId,
      user_id: userId,
      medication_name: medicationName,
      start_of_supply: startISO,
      perform_every_number: n,
      perform_every_unit: unit,
      dose_number: doseNumber,
      dose_unit: doseUnit,
      next_supply: nextSupply,
      created_at: now,
    },
    assignments_created: petIds.length,
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
  const dt = new Date(baseIso);
  if (Number.isNaN(dt.getTime())) throw Object.assign(new Error("Invalid start_of_activity ISO"), {status: 400});
  const n = Number(number);
  if (!Number.isFinite(n)) throw Object.assign(new Error("perform_every_number must be numeric"), {status: 400});
  switch (String(unit || "").toLowerCase()) {
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
      throw Object.assign(new Error("perform_every_unit must be one of hours|days|weeks|months"), {status: 400});
  }
  return dt.toISOString();
}

async function getPetsService() {
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
  const out = rows.slice(1).map(cols => ({
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
  const rawTitle = required(body.title || body.name, "title");
  const title = String(rawTitle).trim();
  const titleRegex = /^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$/;
  if (title.length < 2 || title.length > 50 || !titleRegex.test(title)) {
    const e = new Error("Invalid title"); e.status = 400; throw e;
  }

  const startInput = required(body.start_of_activity, "start_of_activity");
  // Expecting format: YYYY-MM-DD HH:mm (24h)
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
  const startISO = new Date(Date.UTC(yyyy, mm - 1, dd, HH, MM, 0, 0)).toISOString();

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
  const nextActivity = addIntervalISO(startISO, n, unit);

  const sheets = await getSheetsClient();

  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "routines!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      routineId,
      userId,
      title,
      String(n),
      String(unit),
      String(startISO),
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
      last_performed_at: "",
      next_activity: nextActivity,
      assigned_at: now,
    });
  }
  if (assignments.length > 0) {
    const values = assignments.map(a => [
      a.assignment_id,
      a.user_id,
      a.pet_id,
      a.routine_id,
      a.last_performed_at,
      a.next_activity,
      a.assigned_at,
    ]);
    await sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "routineassignments!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: {values},
    });
  }

  const activityId = admin.firestore().collection("_ids").doc().id;
  const details = JSON.stringify({
    title,
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

  return {
    routine: {
      routine_id: routineId,
      user_id: userId,
      title,
      perform_every_number: Number(n),
      perform_every_unit: String(unit),
      start_of_activity: String(startISO),
      next_activity: nextActivity,
      created_at: now,
    },
    assignments,
  };
}

module.exports = {
  getPetsService,
  createPetService,
  createRoutineService,
  createMedicationService,
  createVisitService,
};

async function createVisitService({userId, body}) {
  const petId = required(body.pet_id, "pet_id");

  // Visit Type (required, from predefined list)
  const allowedVisitTypes = new Set(["Routine Checkup", "Vaccination", "Emergency", "Surgery", "Follow-up", "Other"]);
  const visitTypeInput = required(body.visit_type || body.event_name || body.title || body.name, "visit_type");
  const visitType = String(visitTypeInput).trim();
  if (!allowedVisitTypes.has(visitType)) {
    const e = new Error("visit_type must be one of Routine Checkup|Vaccination|Emergency|Surgery|Follow-up|Other"); e.status = 400; throw e;
  }

  // Event name will mirror the visit type (for readability in healthevents)
  const eventName = visitType;

  // Expecting format: YYYY-MM-DD HH:mm (24h) like other services
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
  const startISO = new Date(Date.UTC(yyyy, mm - 1, dd, HH, MM, 0, 0)).toISOString();

  // Optional recurrence
  let n = "";
  let unit = "";
  let nextActivity = "";
  if (body.perform_every_number !== undefined && body.perform_every_unit !== undefined) {
    const nRaw = String(body.perform_every_number).trim();
    if (!/^[0-9]+$/.test(nRaw) || parseInt(nRaw, 10) <= 0) {
      const e = new Error("perform_every_number must be a positive integer"); e.status = 400; throw e;
    }
    n = Number(nRaw);
    const unitRaw = String(body.perform_every_unit || "").toLowerCase();
    const allowedUnits = new Set(["hour", "hours", "day", "days", "week", "weeks", "month", "months"]);
    if (!allowedUnits.has(unitRaw)) {
      const e = new Error("perform_every_unit must be one of hours|days|weeks|months"); e.status = 400; throw e;
    }
    unit = unitRaw;
    nextActivity = addIntervalISO(startISO, n, unit);
  }

  // Clinic / Location (required)
  const clinicRaw = required(body.clinic || body.location, "clinic");
  const clinic = String(clinicRaw).trim();
  if (clinic.length < 2 || clinic.length > 100) {
    const e = new Error("clinic length must be 2-100");
    e.status = 400;
    throw e;
  }
  const clinicRegex = /^[A-Za-z0-9 .`,]+$/;
  if (!clinicRegex.test(clinic)) {
    const e = new Error("clinic contains invalid characters");
    e.status = 400;
    throw e;
  }

  // Veterinarian (optional)
  const vetRaw = body.veterinarian || body.vet || "";
  const veterinarian = String(vetRaw).trim();
  if (veterinarian) {
    if (veterinarian.length < 2 || veterinarian.length > 100) {
      const e = new Error("veterinarian length must be 2-100");
      e.status = 400;
      throw e;
    }
    const vetRegex = /^[A-Za-z '.]+$/;
    if (!vetRegex.test(veterinarian)) {
      const e = new Error("veterinarian contains invalid characters");
      e.status = 400;
      throw e;
    }
  }

  // Notes (optional)
  const notes = body.notes ? String(body.notes).trim() : "";
  if (notes.length > 500) {
    const e = new Error("notes too long");
    e.status = 400;
    throw e;
  }

  const spreadsheetId = ensureSpreadsheetId();
  const sheets = await getSheetsClient();
  const now = isoNow();
  const healthId = admin.firestore().collection("_ids").doc().id;
  const eventType = "vet_visit";

  // healthevents sheet columns:
  // health_id, pet_id, user_id, event_type, event_name, start_of_activity, perform_every_number, perform_every_unit, next_activity, notes, created_at
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "healthevents!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      healthId,
      petId,
      userId,
      eventType,
      eventName,
      startISO,
      n === "" ? "" : String(n),
      unit === "" ? "" : String(unit),
      nextActivity,
      notes,
      now,
    ]]},
  });

  // Activity log
  const activityId = admin.firestore().collection("_ids").doc().id;
  const details = JSON.stringify({
    event_type: eventType,
    event_name: eventName,
    start_of_activity: startISO,
    perform_every_number: n,
    perform_every_unit: unit,
    next_activity: nextActivity,
    clinic,
    veterinarian,
    notes,
  });
  await sheets.spreadsheets.values.append({
    spreadsheetId,
    range: "activitylog!A1",
    valueInputOption: "USER_ENTERED",
    insertDataOption: "INSERT_ROWS",
    requestBody: {values: [[
      activityId,
      userId,
      "CREATE_HEALTHEVENT",
      "healthevents",
      healthId,
      now,
      details,
    ]]},
  });

  return {
    health_id: healthId,
    pet_id: petId,
    user_id: userId,
    event_type: eventType,
    event_name: eventName,
    start_of_activity: startISO,
    perform_every_number: n,
    perform_every_unit: unit,
    next_activity: nextActivity,
    notes,
    created_at: now,
  };
}
