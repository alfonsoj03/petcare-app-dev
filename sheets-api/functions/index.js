const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const {getPetsService, createPetService, createRoutineService, createMedicationService, createVisitService} = require("./services");

setGlobalOptions({maxInstances: 10});

// POST /visits — create a vet visit in healthevents
exports.createVisit = onRequest({cors: true}, async (req, res) => {
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
    const result = await createVisitService({userId, body});
    return res.status(201).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("createVisit failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});

// POST /medications
exports.createMedication = onRequest({cors: true}, async (req, res) => {
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
exports.getPets = onRequest({cors: true}, async (req, res) => {
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
    const result = await createPetService({userId, body});
    return res.status(201).json(result);
  } catch (err) {
    const code = err.status || 500;
    logger.error("createPet failed", err);
    return res.status(code).json({error: err.message || "Internal Error"});
  }
});
