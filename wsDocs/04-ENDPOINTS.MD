## Endpoints / Cloud Functions a desarrollar

1. `POST /routines/{routine_id}/perform`

- **Body**: `{ "pet_id": "UUID", "performed_at": "2025-10-21T14:30:00Z" }`
- **Acción**:
    - Verificar token recibido → obtener user_id.
    - Buscar routine por routine_id y user_id.
        - Si no existe → responder 404 Not Found.
    - Buscar assignment en routineassignments por routine_id + pet_id + user_id.
        - Si no existe → responder 404 Not Found.
    - Obtener de la rutina los valores:
        - perform_every_number
        - perform_every_unit
    - Calcular:
        - next_activity = addInterval(performed_at, perform_every_number, perform_every_unit)
    - Actualizar la fila en routineassignments:
        - last_performed_at = performed_at
        - next_activity = next_activity
    - Insertar una fila en activitylog:
        - action_type = "perform_routine"
        - target_table = "routineassignments"
        - target_id = assignment_id
        - timestamp = performed_at
        - details = { "next_activity": next_activity }
        - user_id = user_id (del token)
    - Responder: `{ "assignment_id": "UUID", "last_performed_at": "2025-10-21T14:30:00Z", "next_activity": "2025-11-04T14:30:00Z" }`
2. `POST /medications/{med_id}/perform`
- **Body**: `{ "pet_id": "UUID", "given_at": "2025-10-21T14:30:00Z" }`
- **Acción**:
    - Verificar token recibido → obtener `user_id`.
    - Buscar medication por `medication_id` y `user_id`.
        - Si no existe → responder 404 Not Found.
    - Buscar assignment en `medicationassignments` por `medication_id` + `pet_id` + `user_id`.
        - Si no existe → responder 404 Not Found.
    - Obtener del medicamento los valores:
        - `perform_every_number`
        - `perform_every_unit`
    - Calcular:
        - `next_supply = addInterval(given_at, perform_every_number, perform_every_unit)`
    - Actualizar la fila en `medicationassignments`:
        - `last_given_at = given_at`
        - `next_supply = next_supply`
    - Insertar una fila en `activitylog`:
        - `action_type = "perform_medication"`
        - `target_table = "medicationassignments"`
        - `target_id = assignment_id`
        - `timestamp = given_at`
        - `details = { "next_supply": next_supply }`
        - `user_id = user_id` (del token)
    - Responder: `{ "assignment_id": "UUID", "last_given_at": "2025-10-21T14:30:00Z", "next_supply": "2025-10-21T16:30:00Z" }`
3. `GET /pets/{pet_id}/next-events?limit=N`
- **Query**:
    - `user_id` obtenido desde token.
    - `limit` (por defecto `3`).
- **Acción**:
    - Buscar próximas rutinas:
        - De `routineassignments` donde:
            - `pet_id = {pet_id}`
            - `user_id = {user_id}`
            - `next_activity >= NOW()`
    - Buscar próximas medicaciones:
        - De `medicationassignments` donde:
            - `pet_id = {pet_id}`
            - `user_id = {user_id}`
            - `next_supply >= NOW()`
            - `is_completed = false`
    - Buscar próximos eventos de salud:
        - De `healthevents` donde:
            - `pet_id = {pet_id}`
            - `user_id = {user_id}`
            - `start_of_activity >= NOW()`
    - Combinar resultados en una lista común con formato:
        `{ "type": "routine" | "medication" | "healthevent", "source_id": "UUID", "title": "Nombre de la rutina / medicamento / evento", "datetime": "2025-10-28T14:30:00Z" }`
    - Ordenar por `datetime` ascendente.
    - Limitar a los primeros `N` resultados (por defecto `3`).
    - Responder con la lista resultante.
- **Ejemplo de respuesta**: `[{ "type": "routine", "source_id": "routineassign-abc123", "title": "Baño", "datetime": "2025-10-28T14:30:00Z" }, { "type": "medication", "source_id": "medassign-def456", "title": "Antiparasitario", "datetime": "2025-10-29T09:00:00Z" }, { "type": "healthevent", "source_id": "healthevent-ghi789", "title": "Control veterinario", "datetime": "2025-11-01T16:00:00Z" }]`
4. `GET /pets`
- **Auth**: No requiere autenticación (demo).
- **Acción**:
    - Devuelve todas las filas de la hoja `pets` mapeadas a:
        - `pet_id`
        - `user_id`
        - `name`
        - `imageUrl`
        - `species`
        - `sex`
        - `breed`
        - `date_of_birth`
        - `weight_kg`
        - `color`
        - `created_at`
- **Responder**: `[{ pet_id, user_id, name, imageUrl, species, sex, breed, date_of_birth, weight_kg, color, created_at }]`

5. CRUD básicos: 
AHORA QUEDAMOS EN QUE ESTO SE DEBE VERIFICAR QUE SE HAGA CON EL SELECTED PET.

`POST /pets`
`PUT /pets/{pet_id}`
`POST /routines`
`PUT /routines/{id}`
`POST /medications`
`PUT /medications/{id}` 

- Deben crear filas en las hojas correspondientes y, si el payload incluye `assign_to_pets`, crear filas en `routineassignments` o `medicationassignments` con `assigned_at = now` y calcular `next` a partir de `start_of_activity` o `assigned_at`.

6. `GET /medications/{petid}` , `GET /ROUTINES/{petid}`
    - Devuelve lista de rutinas o medicamentos del `user_id`. Esto para mostrar en la pestaña correspondiente de cada mascota en la app.