# 🐾 PetCare App

**PetCare** es una aplicación móvil desarrollada en **Kotlin (Android Studio)** que permite gestionar la salud, cuidados y rutinas de tus mascotas.  
Su objetivo es ofrecer un espacio centralizado donde los dueños puedan registrar vacunas, visitas al veterinario, medicamentos, alimentación y otras tareas rutinarias.

---

## 🚀 Características principales

### 🐶 Gestión de Mascotas
- Registro de mascotas con nombre, especie, raza, fecha de nacimiento, sexo, peso y color.
- Opción de añadir y actualizar fotos desde la galería.
- Listado general de mascotas con su estado de salud o vacunación.

### 💉 Salud y Vacunación
- Control de vacunas con fecha de última aplicación y próxima dosis.
- Adjuntar certificados de vacunación en formato digital.
- Registro de tratamientos o medicamentos en curso (nombre, dosis, fechas).
- Seguimiento de visitas al veterinario con recordatorios configurables.

### 🛁 Cuidados Rutinarios
- Control de actividades periódicas como baño, cambio de arena, cuidado dental o alimentación.
- Posibilidad de marcar actividades como completadas.
- Opción de realizar cuidados anticipados: el sistema recalcula automáticamente la siguiente fecha.

### 🩺 Visitas Veterinarias Inteligentes
- Planificación de visitas con recordatorios automáticos.
- Permite registrar **visitas extraordinarias** (fuera del calendario) por síntomas o emergencias.
- Reajuste dinámico del calendario de visitas tras cada registro.

### 👤 Perfil del Usuario
- Gestión de cuenta personal y configuración básica.
- Opciones de notificaciones, idioma y seguridad.
- Cierre de sesión seguro.

---

## 🧭 Flujo general de navegación

1. **Dashboard (Inicio)**  
   Vista principal con resumen de la mascota seleccionada: próxima vacuna, visita o tarea pendiente.  
   Acceso rápido a registros de eventos o secciones clave.

2. **My Pets**  
   Listado completo de mascotas registradas.  
   Permite editar, eliminar o añadir nuevas mascotas.

3. **Add / Edit Pet**  
   Formulario para registrar o actualizar la información de una mascota.

4. **Routine**  
   Panel para registrar y controlar tareas rutinarias (baños, alimentación, medicación, etc.).

5. **Health**  
   Sección de salud: vacunas, tratamientos, visitas al veterinario y certificados.

6. **Profile**  
   Configuración de la cuenta del usuario, notificaciones y privacidad.

---

## 🧱 Stack tecnológico

| Componente | Descripción |
|-------------|--------------|
| **Lenguaje** | Kotlin |
| **Framework UI** | Jetpack Compose |
| **Arquitectura sugerida** | MVVM (Model-View-ViewModel) |
| **Autenticación** | Firebase Authentication (Google Sign-In) |
| **Almacenamiento multimedia** | Firebase Storage |
| **Recordatorios / Notificaciones** | Android AlarmManager + WorkManager |
| **Backend** | Google Sheets + Apps Script (para respaldo o análisis) |

---