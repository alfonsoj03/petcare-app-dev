/* eslint-disable */
const {setGlobalOptions} = require("firebase-functions/v2");
const {onRequest} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const {getPetsService, createPetService, updatePetService, createRoutineService, createMedicationService, createVisitService, performRoutineService, performMedicationService, getPetNextEventsService, getRoutinesByPetService, updateRoutineService, createOrUpdateUserService, deleteRoutineService, getMedicationsByPetService, deleteMedicationService} = require("./services");

setGlobalOptions({maxInstances: 10});

// POST /deleteMedication — delete medication and all its assignments for the user
exports.deleteMedication = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
    // Auth
    let userId;
    const allowBypass = process.env.ALLOW_INSECURE_EMULATOR === "1";
    if (allowBypass) {
      userId = (req.headers["x-debug-uid"] || req.headers["X-Debug-Uid"] || "dev-user") + "";
    } else {
      const token = bearer(req);
      if (!token) return res.status(401).json({error: "Missing Bearer token"});
      const decoded = await admin.auth().verifyIdToken(token);
      userId = decoded.uid;
    }

    const result = await deleteMedicationService({userId, body: req.body || {}});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("deleteMedication failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// GET /medications?pet_id=XYZ — list medications for a pet (auth like getRoutines)
// Also supports Hosting rewrites path: /pets/{petId}/medications
exports.getMedications = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "GET") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
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
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }

    // petId from query or from path (for Hosting rewrite /pets/:id/medications)
    let petId = req.query && req.query.pet_id ? String(req.query.pet_id) : "";
    if (!petId && req.path) {
      const m = req.path.match(/\/pets\/([^/]+)\/medications/i);
      if (m && m[1]) petId = m[1];
    }
    if (!petId) return res.status(400).json({error: "pet_id is required"});

    const data = await getMedicationsByPetService({userId, petId});
    return res.status(200).json(data);
  } catch (err) {
    const code = err.status || 500;
    logger.error("getMedications failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /deleteRoutine — delete routine and all its assignments for the user
exports.deleteRoutine = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
    // Auth
    let userId;
    const allowBypass = process.env.ALLOW_INSECURE_EMULATOR === "1";
    if (allowBypass) {
      userId = (req.headers["x-debug-uid"] || req.headers["X-Debug-Uid"] || "dev-user") + "";
    } else {
      const token = bearer(req);
      if (!token) return res.status(401).json({error: "Missing Bearer token"});
      const decoded = await admin.auth().verifyIdToken(token);
      userId = decoded.uid;
    }

    const result = await deleteRoutineService({userId, body: req.body || {}});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("deleteRoutine failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /deleteMedication — delete medication and all its assignments for the user
exports.deleteMedication = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
    // Auth
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
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }

    const body = typeof req.body === "string" ? JSON.parse(req.body || "{}") : (req.body || {});
    const result = await deleteMedicationService({userId, body});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("deleteMedication failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// GET /diagnostics — returns current config info (for debugging)
exports.diagnostics = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    const spreadsheetId = process.env.SPREADSHEET_ID || "";
    // Optionally mask in production; for dev we return plainly
    return res.status(200).json({spreadsheet_id: spreadsheetId});
  } catch (err) {
    const code = err.status || 500;
    logger.error("diagnostics failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// GET /routines?pet_id=XYZ — list routines for a pet (requires auth unless emulator bypass)
exports.getRoutines = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "GET") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
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
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }
    const petId = req.query && req.query.pet_id ? String(req.query.pet_id) : "";
    if (!petId) return res.status(400).json({error: "pet_id is required"});
    const data = await getRoutinesByPetService({userId, petId});
    return res.status(200).json(data);
  } catch (err) {
    const code = err.status || 500;
    logger.error("getRoutines failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /createUser — verify token and upsert user in Google Sheets
exports.createUser = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
    const token = bearer(req);
    if (!token) return res.status(401).json({error: "Missing Bearer token"});
    let decoded;
    try {
      decoded = await admin.auth().verifyIdToken(token);
    } catch (err) {
      logger.error("createUser token verify failed", err);
      return res.status(401).json({error: "Invalid token"});
    }
    const userId = decoded.uid;
    const email = decoded.email || (req.body && (typeof req.body === 'string' ? JSON.parse(req.body).email : req.body.email)) || "";
    const name = decoded.name || (req.body && (typeof req.body === 'string' ? JSON.parse(req.body).name : req.body.name)) || "";
    const result = await createOrUpdateUserService({userId, email, name});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("createUser failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// PUT /routines — update a routine assignment for a specific pet
exports.updateRoutine = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "PUT") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
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
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }
    const body = typeof req.body === "string" ? JSON.parse(req.body || "{}") : (req.body || {});
    const result = await updateRoutineService({userId, body});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("updateRoutine failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// PUT /updatePet
exports.updatePet = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "PUT") {
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
    const result = await updatePetService({userId, body});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("updatePet failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// GET /pets/{pet_id}/next-events?limit=N — upcoming events for a pet
exports.getPetNextEvents = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "GET") {
      return res.status(405).json({error: "Method Not Allowed"});
    }

    // Auth (supports emulator bypass)
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
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }

    const petId = req.query && req.query.pet_id ? String(req.query.pet_id) : "";
    if (!petId) {
      return res.status(400).json({error: "pet_id is required"});
    }
    const limit = req.query && req.query.limit ? Number(req.query.limit) : undefined;

    const result = await getPetNextEventsService({userId, petId, limit});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("getPetNextEvents failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /medications/{med_id}/perform — mark medication given for a pet
exports.performMedication = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({error: "Method Not Allowed"});
    }

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
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }

    const bodyRaw = typeof req.body === "string" ? JSON.parse(req.body || "{}") : (req.body || {});
    const medId = (req.query && (req.query.medication_id || req.query.med_id)) ? String(req.query.medication_id || req.query.med_id) : (bodyRaw.medication_id || bodyRaw.med_id || bodyRaw.id);
    const body = Object.assign({}, bodyRaw, {medication_id: medId});
    const result = await performMedicationService({userId, body});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("performMedication failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /routines/{routine_id}/perform — mark routine performed for a pet
exports.performRoutine = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({error: "Method Not Allowed"});
    }

    // Auth (supports emulator bypass)
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
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }

    const bodyRaw = typeof req.body === "string" ? JSON.parse(req.body || "{}") : (req.body || {});
    const routineId = (req.query && req.query.routine_id) ? String(req.query.routine_id) : (bodyRaw.routine_id || bodyRaw.id);
    const body = Object.assign({}, bodyRaw, {routine_id: routineId});
    const result = await performRoutineService({userId, body});
    return res.status(200).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("performRoutine failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /medications
exports.createMedication = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
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
    const result = await createMedicationService({userId, body});
    return res.status(201).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("createMedication failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// GET /pets — list all pets (demo: no auth required)
exports.getPets = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
  try {
    if (req.method !== "GET") {
      return res.status(405).json({error: "Method Not Allowed"});
    }
    const out = await getPetsService();
    return res.json(out);
  } catch (err) {
    const code = err.status || 500;
    logger.error("getPets failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /routines — create routine + assignments + activity log
exports.createRoutine = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
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
      try {
        decoded = await admin.auth().verifyIdToken(token);
      } catch (err) {
        return res.status(401).json({error: "Invalid token"});
      }
      userId = decoded.uid;
    }
    const body = typeof req.body === "string" ? JSON.parse(req.body || "{}") : (req.body || {});
    const result = await createRoutineService({userId, body});
    return res.status(201).json(result);
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

function bearer(req) {
  const h = req.headers["authorization"] || req.headers["Authorization"];
  if (!h) return null;
  const parts = h.split(" ");
  if (parts.length === 2 && parts[0] === "Bearer") return parts[1];
  return null;
}

// POST /createPet
exports.createPet = onRequest({cors: true, secrets: ["SPREADSHEET_ID"]}, async (req, res) => {
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
    const result = await createPetService({userId, body});
    return res.status(201).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("createPet failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});