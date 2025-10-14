# 🐾 START HERE — Workspace Initialization Guide

Welcome to the PetCare Companion project workspace.

This file serves as the **entry point for agents or collaborators** to understand where to find key resources, documentation, and project structure.

---

## 🗂️ Project Directory Map

/
├── app/ # Native Android source code (Kotlin)
│ ├── src/main/
│ │ ├── java/com/example/mascotasapp # App logic & architecture (Activities, ViewModels, etc.)
│ │ ├── res/ # Layouts, strings, icons, themes
│ │ └── AndroidManifest.xml
│ └── build.gradle.kts
│
├── wsDocs/ # 🧠 Workspace for documentation and AI context
│ ├── [00-START-HERE.md](http://00-start-here.md/) # This file → initialization entry point
│ ├── [01-JOURNEY.md](http://journey.md/) # Development sprints, backlog, and roadmap
│ ├── [02-WORKLOG.md](http://work-log.md/) # Technical log of changes, commits, and refactors
│ ├── images/ # Wireframes and UI mockups for reference
│
├── .gitignore
├── [README.md](http://readme.md/) # Public-facing project description
└── build.gradle.kts

---

## 📍 Purpose of Each Document

| File | Purpose |
| --- | --- |
| [**00-START-HERE.md**](http://00-start-here.md/) | Initialization prompt for AI or collaborators. Explains structure, context, and how to proceed. |
| [**01-JOURNEY.md**](http://journey.md/) | Contains all sprints, milestones, and pending tasks. Used to define current focus. |
| [**02-WORKLOG.md**](http://work-log.md/) | Tracks technical updates: commits, bug fixes, architectural changes. Each log entry should be dated and summarized. |
| **images/** | Contains wireframes and conceptual diagrams for all 5 screens of the PetCare Companion app. |
| [**/README.md](http://readme.md/) (root)** | Public summary of what the app does, its goals, other technical information. |

---

## 🧩 Current Project Context

**Project Name:** PetCare Companion

**Platform:** Native Android (Kotlin)

**Backend / Cloud:** Google Sheets + Functions | Firebase

**Goal:** Lightweight yet realistic pet management app — includes reminders, vaccination logs, veterinary visits, and care routines.

**Main Screens:**

1. **Dashboard / Home:** summary view with next care actions (vaccines, vet, tasks).
2. **Pet Profile:** basic info form (name, species, date of birth, etc.).
3. **Health & Vaccination:** vaccination log, medications, and veterinary visits.
4. **Routine Care:** recurring care reminders (bath, feeding, litter changes).
5. **Profile:** Profile, notifications, privacy and security, log-out.

---

## 🧭 Workflow Reference for Agents

When initializing a working session, follow this order:

1. **Start here** → read `00-START-HERE.md` to understand current context.
2. **Then open** → [`01-JOURNEY.md`](https://www.notion.so/journey.md)
    - Check current sprint goals and the active section marked as `▶ CURRENT FOCUS`.
    - Identify what module or feature you should work on next.
3. **Log your work in** → [`02-WORKLOG.md`](https://www.notion.so/work-log.md)
    - Add your changes under a new dated entry.
    - Include what was modified, reasoning, and dependencies affected.
4. **Check visual guidance** → [`images/`](https://www.notion.so/images/)
    - Review wireframes before building or editing any UI.
    - All design assets (mockups, diagrams, etc.) are stored here.

---

## 🧱 Development Environment

- **Language:** Kotlin (Android)
- **IDE:** Android Studio
- **UI Framework:** Jetpack Compose + Material Design 3
- **Backend Stack:**
  - **Google Sheets + Apps Script / Cloud Functions:**  
    Utilizados como backend principal para el almacenamiento estructurado de datos (mascotas, rutinas, vacunas, visitas al veterinario, etc.).  
    Las operaciones CRUD se manejan mediante funciones personalizadas que exponen endpoints seguros consumidos desde la app.
  - **Firebase Storage:**  
    Usado únicamente para el almacenamiento de archivos (fotos de mascotas, certificados de vacunas, documentos veterinarios).  
    Cada archivo almacenado se referencia en la hoja correspondiente de Google Sheets mediante su URL pública o protegida.
- **(Opcional)** `firebase-messaging` puede integrarse posteriormente para manejar recordatorios o notificaciones automáticas.

🔧 Asegúrate de declarar todas las dependencias necesarias en `app/build.gradle.kts`.

---

## 🚀 Initialization Tasks (Sprint 0)

| Task | Description | Status |
| --- | --- | --- |
| ✅ | Create conceptual structure and wireframes | done |
| ✅ | Set up `.gitignore` and `/wsDocs` structure | done |
| ✅ | Initialize Android project (Empty Activity) | done |

---

## 🧩 Next Step

> → Open journey.md
> 
> 
> Identify the next sprint and begin work on **Firebase setup and dashboard layout**.
> 
> Once a task is completed, log all details in [`work-log.md`].
> 

---