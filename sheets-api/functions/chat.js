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

  async function createRoutineService({userId, body}) {
    const rawRoutineName = required(body.routine_name || body.title || body.name, "routine_name");
    const routineName = String(rawRoutineName).trim();
    const nameRegex = /^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$/;
    if (routineName.length < 2 || routineName.length > 50 || !nameRegex.test(routineName)) {
      const e = new Error("Invalid routine_name"); e.status = 400; throw e;
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
        routineName,
        String(startISO),
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
        last_performed_at: "",
        next_activity: nextActivity,
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
        valueInputOption: "USER_ENTERED",
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
  
    return {
      routine: {
        routine_id: routineId,
        user_id: userId,
        routine_name: routineName,
        perform_every_number: Number(n),
        perform_every_unit: String(unit),
        start_of_activity: String(startISO),
        next_activity: nextActivity,
        created_at: now,
      },
      assignments,
    };
  }