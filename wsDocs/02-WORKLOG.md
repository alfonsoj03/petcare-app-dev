# 🧠 Work Log — PetCare App

Registro estructurado de los avances técnicos y decisiones de diseño durante el desarrollo del proyecto. **Este documento debe ser escrito en su totalidad en español.**

---

## 📅 Estructura del Worklog

Cada entrada contiene:

- **Fecha:** día del cambio o commit importante.
- **Sprint:** referencia al sprint en `journey.md`.
- **Módulo afectado:** `frontend`, `backend`, `firebase`, `ui`, etc.
- **Descripción breve:** resumen del trabajo realizado.
- **Detalles técnicos:** pasos, decisiones, configuraciones o librerías implicadas.
- **Impacto / Resultado esperado:** qué mejora o función se añade.

---

## 2025-10-12 — Sprint 1 — UI (Tipografías locales)

- **Descripción breve:** Las tipografías de la app se configuraron para ser **locales (TTF)**, sin requerir descarga desde la nube.
- **Recursos (colocar en `app/src/main/res/font/`):** `nunito_variable.ttf`, `inter_variable.ttf`, `itim_regular.ttf`.
- **Archivos actualizados:** `app/src/main/java/com/example/mascotasapp/ui/theme/Type.kt`.
- **Resultado:** La app usa fuentes locales en todo momento.

---

## 2025-10-12 — Sprint 2 — UI (Dashboard)

- **Módulo afectado:** `frontend`, `ui`
- **Descripción breve:** Se alineó la pantalla de Dashboard al wireframe (`/wsDocs/wireframes/dashboard.png`).
- **Detalles técnicos:**
  - Se hicieron las tarjetas de resumen clicables y con callbacks de navegación: `onOpenHealth`, `onOpenRoutine`.
  - Se añadieron íconos a las acciones rápidas y se expusieron callbacks: `onRegisterBath`, `onRegisterVisit`, `onRegisterFeeding`.
  - Se reemplazó `Divider` deprecado por `HorizontalDivider`.
  - Se añadieron imports de `Icons.*` necesarios.
- **Archivos actualizados:**
  - `app/src/main/java/com/example/mascotasapp/ui/screens/dashboard/DashboardScreen.kt`
- **Impacto / Resultado esperado:**
  - Dashboard más cercano al wireframe con navegación desde tarjetas y acciones rápidas más descriptivas.
  - Eliminación de deprecaciones visibles en la sección de historial.