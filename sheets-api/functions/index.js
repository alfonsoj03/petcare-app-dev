/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const {google} = require("googleapis");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

// GET /pets — list all pets (demo: no auth required)
exports.getPets = onRequest({cors: true}, async (req, res) => {
  try {
    if (req.method !== "GET") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
    const spreadsheetId = process.env.SPREADSHEET_ID;
    if (!spreadsheetId) {
      return res.status(500).json({error: "Missing SPREADSHEET_ID env var"});
    }
    const sheets = await getSheetsClient();
    const r = await sheets.spreadsheets.values.get({
      spreadsheetId,
      range: "pets!A1:K10000", // up to created_at
      valueRenderOption: "UNFORMATTED_VALUE",
      dateTimeRenderOption: "FORMATTED_STRING",
    });
    const rows = r.data.values || [];
    if (rows.length <= 1) return res.json([]);
    const header = rows[0].map(String);
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
    return res.json(out);
  } catch (err) {
    const code = err.status || 500;
    logger.error("getPets failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /routines — create routine + assignments + activity log
exports.createRoutine = onRequest({cors: true}, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({error: "Method Not Allowed"});
    }

    // Emulator bypass
    let userId;
    const allowBypass = process.env.ALLOW_INSECURE_EMULATOR === "1";
    if (allowBypass) {
      userId = (req.headers["x-debug-uid"] || req.headers["X-Debug-Uid"] || "dev-user") + "";
    } else {
      const token = bearer(req);
      if (!token) return res.status(401).json({error: "Missing Bearer token"});
      let decoded;
      try { decoded = await admin.auth().verifyIdToken(token); } catch (err) {
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }

    const body = typeof req.body === "string" ? JSON.parse(req.body || "{}") : (req.body || {});
    const title = required(body.title || body.name, "title");
    const start = required(body.start_of_activity, "start_of_activity");
    const everyNum = required(body.perform_every_number, "perform_every_number");
    const everyUnit = required(body.perform_every_unit, "perform_every_unit");
    const petIds = Array.isArray(body.assign_to_pets) ? body.assign_to_pets.map(String) : [];

    const spreadsheetId = process.env.SPREADSHEET_ID;
    if (!spreadsheetId) {
      return res.status(500).json({error: "Missing SPREADSHEET_ID env var"});
    }

    const now = isoNow();
    const routineId = admin.firestore().collection("_ids").doc().id;
    const nextActivity = addIntervalISO(start, everyNum, everyUnit);

    const sheets = await getSheetsClient();

    // 1) Insert routine (assumed header order): routine_id, user_id, title, perform_every_number, perform_every_unit, start_of_activity, created_at
    await sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "routines!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: {values: [[
        routineId,
        userId,
        title,
        String(everyNum),
        String(everyUnit),
        String(start),
        now,
      ]]},
    });

    // 2) Insert assignments for each pet
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
      // assumed header order: assignment_id, user_id, pet_id, routine_id, last_performed_at, next_activity, assigned_at
      await sheets.spreadsheets.values.append({
        spreadsheetId,
        range: "routineassignments!A1",
        valueInputOption: "USER_ENTERED",
        insertDataOption: "INSERT_ROWS",
        requestBody: {values},
      });
    }

    // 3) Log activity
    const activityId = admin.firestore().collection("_ids").doc().id;
    // assumed header: activity_id, user_id, action_type, target_table, target_id, timestamp, details
    const details = JSON.stringify({title, start_of_activity: start, perform_every_number: everyNum, perform_every_unit: everyUnit, assign_to_pets: petIds});
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

    return res.status(201).json({
      routine: {
        routine_id: routineId,
        user_id: userId,
        title,
        perform_every_number: Number(everyNum),
        perform_every_unit: String(everyUnit),
        start_of_activity: String(start),
        next_activity: nextActivity,
        created_at: now,
      },
      assignments,
    });
  } catch (err) {
    const code = err.status || 500;
    logger.error("createRoutine failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp();
}

// Helpers
const SCOPES = ["https://www.googleapis.com/auth/spreadsheets"]; 
async function getSheetsClient() {
  // When running in Firebase, ADC will use the Functions service account.
  const auth = await google.auth.getClient({scopes: SCOPES});
  return google.sheets({version: "v4", auth});
}

function bearer(req) {
  const h = req.headers["authorization"] || req.headers["Authorization"];
  if (!h) return null;
  const parts = h.split(" ");
  if (parts.length === 2 && parts[0] === "Bearer") return parts[1];
  return null;
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
  case "minute":
  case "minutes":
    dt.setMinutes(dt.getMinutes() + n); break;
  case "hour":
  case "hours":
    dt.setHours(dt.getHours() + n); break;
  case "day":
  case "days":
    dt.setDate(dt.getDate() + n); break;
  case "week":
  case "weeks":
    dt.setDate(dt.getDate() + 7 * n); break;
  default:
    throw Object.assign(new Error("perform_every_unit must be one of minutes|hours|days|weeks"), {status: 400});
  }
  return dt.toISOString();
}

// POST /pets
exports.createPet = onRequest({cors: true}, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({error: "Method Not Allowed"});
    }

    // Emulator bypass: if ALLOW_INSECURE_EMULATOR=1, allow X-Debug-Uid header
    let userId;
    const allowBypass = process.env.ALLOW_INSECURE_EMULATOR === "1";
    if (allowBypass) {
      userId = (req.headers["x-debug-uid"] || req.headers["X-Debug-Uid"] || "dev-user") + "";
    } else {
      const token = bearer(req);
      if (!token) return res.status(401).json({error: "Missing Bearer token"});
      let decoded;
      try {
        decoded = await admin.auth().verifyIdToken(token);
      } catch (err) {
        logger.error("Auth verification failed", err);
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }
    const body = typeof req.body === "string" ? JSON.parse(req.body || "{}") : (req.body || {});

    const name = required(body.name, "name");
    const species = required(body.species, "species");
    const sex = required(body.sex, "sex");
    const breed = body.breed ? String(body.breed) : "";
    const dob = body.dob ? String(body.dob) : ""; // ISO or any string; backend stores raw
    const weight = body.weight ? String(body.weight) : "";
    const color = body.color ? String(body.color) : "";
    const imageUrl = body.imageUrl ? String(body.imageUrl) : "";

    const spreadsheetId = process.env.SPREADSHEET_ID;
    if (!spreadsheetId) {
      return res.status(500).json({error: "Missing SPREADSHEET_ID env var"});
    }

    // Prepare row: must match Sheet header order exactly
    // Header expected: pet_id, user_id, name, image_url, species, sex, breed, date_of_birth, weight_kg, color, created_at
    const petId = admin.firestore().collection("_ids").doc().id; // random id gen
    const now = isoNow();
    const values = [[
      petId,      // pet_id
      userId,     // user_id
      name,       // name
      imageUrl,   // image_url
      species,    // species
      sex,        // sex
      breed,      // breed
      dob,        // date_of_birth
      weight,     // weight_kg
      color,      // color
      now,        // created_at
    ]];

    // Append to Google Sheet 'pets' tab
    const sheets = await getSheetsClient();
    await sheets.spreadsheets.values.append({
      spreadsheetId,
      range: "pets!A1",
      valueInputOption: "USER_ENTERED",
      insertDataOption: "INSERT_ROWS",
      requestBody: {values},
    });

    // Respond
    return res.status(201).json({
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
    });
  } catch (err) {
    const code = err.status || 500;
    logger.error("createPet failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});
