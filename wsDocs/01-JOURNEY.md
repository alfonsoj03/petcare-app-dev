# 🧭 JOURNEY.md — PetCare App Development Plan

**Project:** PetCare App

**Platform:** Android (Kotlin)

**Backend:** Firebase + Google Sheets

**Documentation Path:** `/wsDocs/`

---

## 📂 Folder Structure Reference

| Path | Description |
| --- | --- |
| `/app/` | Main Android project (frontend Kotlin) |
| `/backend/` | Scripts for Firebase Functions + Google Sheets integration |
| `/wsDocs/` | Documentation folder (this file, START HERE, worklog, wireframes) |
| `/wsDocs/wireframes/` | Wireframes for each screen |
| `/wsDocs/worklog.md` | Development log |
| `/wsDocs/00-START-HERE.md` | Context initialization for AI agents |

---

## 🚀 PHASE 1 — FRONTEND (Android Studio / Kotlin)

---

### **Sprint 1 — Base Project Setup**

**Goal:** Initialize Android Studio project with required dependencies and structure.

**Tasks:**

- [x]  Create new Android Studio project `PetCareApp`.
- [x]  Add Material Design 3.
- [x]  Add Coil dependency for image handling.
- [x]  Define base color theme and typography.
- [x]  Create repository and add initial commit.

**Reference:**

*(No wireframe – setup stage)*

---

### **Sprint 2 — Dashboard View**

**Goal:** Main dashboard showing pet overview and quick actions.

**Tasks:**

- [ ]  Create `DashboardFragment.kt`.
- [ ]  Display pet photo, name, and species.
- [ ]  Add cards:
    - [ ]  Next vaccine.
    - [ ]  Next vet visit.
    - [ ]  Next routine task.
- [ ]  Add “Quick Action” buttons (Register Bath, Visit, Feeding).
- [ ]  Add event history preview (last 3 actions).
- [ ]  Connect navigation to Health & Routine views.

**Wireframe:**

📸 `/wsDocs/wireframes/dashboard.png`

---

### **Sprint 3 — Pet Management (My Pets + Add/Edit)**

**Goal:** Allow full CRUD operations on pet profiles.

**Tasks:**

- [ ]  Create `MyPetsFragment.kt`:
    - [ ]  Display all pets with photo + status badge (“Up to date” / “Pending”).
    - [ ]  Add “Add new pet” button.
- [ ]  Create `AddPetFragment.kt`:
    - [ ]  Form fields: Name, Species, Race, DOB, Sex, Weight, Color, Photo.
    - [ ]  Add image picker (gallery).
    - [ ]  Validate input.
- [ ]  Create `EditPetFragment.kt` with same form for updates.
- [ ]  Connect navigation between these screens.

**Wireframes:**

📸 `/wsDocs/wireframes/my_pets.png`

📸 `/wsDocs/wireframes/add_pet.png`

📸 `/wsDocs/wireframes/edit_pet.png`

---

### **Sprint 4 — Routine View**

**Goal:** Manage recurring care activities (daily/weekly).

**Tasks:**

- [ ]  Create `RoutineFragment.kt`.
- [ ]  Display recurring activities:
    - [ ]  Bath.
    - [ ]  Dental care.
    - [ ]  Feeding.
    - [ ]  Medications.
- [ ]  Enable “Mark as done”.
- [ ]  Add option “Perform early” → recalculate next reminder.
- [ ]  Store timestamps locally (to be later synced with backend).
- [ ]  Add button “Add custom activity”.

**Wireframe:**

📸 `/wsDocs/wireframes/routine.png`

---

### **Sprint 5 — Health View**

**Goal:** Manage vaccines, medications, and vet visits.

**Tasks:**

- [ ]  Create `HealthFragment.kt`.
- [ ]  Section: **Vaccines**
    - [ ]  List vaccine name, last date, next date.
    - [ ]  Upload certificate (image/pdf).
- [ ]  Section: **Medications**
    - [ ]  List active medications, dosage, range.
- [ ]  Section: **Vet Visits**
    - [ ]  Show next scheduled visit.
    - [ ]  Add “Register Extraordinary Visit” → new form:
        - [ ]  Date.
        - [ ]  Motive.
        - [ ]  Medications prescribed.
    - [ ]  Recalculate next visit from today.

**Wireframe:**

📸 `/wsDocs/wireframes/health.png`

---

### **Sprint 6 — Profile View**

**Goal:** Manage user account and app settings.

**Tasks:**

- [ ]  Create `ProfileFragment.kt`.
- [ ]  Add editable fields (Name, Email, Photo).
- [ ]  Toggle notifications on/off.
- [ ]  Privacy and security settings.
- [ ]  “About App” and “Sign Out” options.
- [ ]  Link to Firebase Authentication.

**Wireframe:**

📸 `/wsDocs/wireframes/profile.png`

---

## ☁️ PHASE 2 — BACKEND (Firebase + Google Sheets)

---

### **Sprint 7 — Backend Initialization**

**Goal:** Create Firebase project and connect with Google Sheets.

**Tasks:**

- [ ]  Initialize Firebase project (create project, initialize console).
- [ ]  Enable Firestore, Authentication, and Storage.
- [ ]  Configure Firebase SDK in Android Studio (`google-services.json`).
- [ ]  Create Firestore collections:
    - [ ]  `/users`
    - [ ]  `/pets`
    - [ ]  `/routines`
    - [ ]  `/health_records`
- [ ]  Prepare Google Sheet as backend mirror for testing.
- [ ]  Set up Apps Script endpoint to read/write data.

**Reference:**

*(No wireframe – backend setup stage)*

---

### **Sprint 8 — Data Sync & Core Functions**

**Goal:** Automate data synchronization and key operations.

**Tasks:**

- [ ]  Write Firebase Functions for:
    - [ ]  Update next vet visit after “Extraordinary Visit”.
    - [ ]  Adjust next routine after early completion.
- [ ]  Sync with Google Sheets:
    - [ ]  Update pet data and events automatically.
    - [ ]  Log all health/routine events.
- [ ]  Implement Firebase Cloud Messaging (FCM) for daily reminders.
- [ ]  Test full cycle between app ↔ Firestore ↔ Sheets.

---

### **Sprint 9 — Backend Intelligence & Automation**

**Goal:** Add intelligent scheduling and data refinement.

**Tasks:**

- [ ]  Define adaptive intervals (dynamic reminders).
- [ ]  Record statistics of routines (e.g., average delay).
- [ ]  Create admin tab in Sheets for manual data review.
- [ ]  Ensure data validation between Firestore and Sheets.

---

## 🧪 PHASE 3 — QA & DEPLOYMENT

---

### **Sprint 10 — Final QA and Beta Release**

**Goal:** Prepare stable beta version for testing.

**Tasks:**

- [ ]  Test all screens and flows (manual + automated).
- [ ]  Validate backend integration and notification flow.
- [ ]  Fix minor bugs and UI inconsistencies.
- [ ]  Generate signed APK.
- [ ]  Upload Beta build to Play Store (closed testing).

---

## 📎 Linked Documents

| File | Description |
| --- | --- |
| `/wsDocs/00-START-HERE.md` | Initialization context |
| `/wsDocs/worklog.md` | Dev log and progress tracking |
| `/wsDocs/wireframes/` | UI visual references |