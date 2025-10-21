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
    - **Body**: `{ pet_id, given_at (ISO) }`
    - **Acción**: igual flujo que arriba, usando `medications` para intervalo y escribiendo en `medicationassignments.last_given_at` y `.next_supply`.
3. `GET /pets/{pet_id}/next-events?limit=N`
    - **Query**: `user_id` desde token, `limit` default 3.
    - **Acción**:
        - Recolectar próximas fechas:
            - `routineassignments.next_activity` para `pet_id` y `user_id` (futuras).
            - `medicationassignments.next_supply` para `pet_id` y `user_id` (futuras, `end_of_supply` > now).
            - `healthevents` con `start_of_activity >= now`.
        - Merge, sort por fecha ascendente, devolver top `N` (cada item: `{ type, source_id, title, datetime }`).
4. `GET /pets`
    - Devuelve lista de pets del `user_id`. Para cada pet, añadir `next_crucial_event` (llamada al `GET /pets/{pet_id}/next-events?limit=1`).
5. CRUD básicos: 

`POST /pets`

`PUT /pets/{pet_id}`

`POST /routines`

`PUT /routines/{id}`

`POST /medications`

`PUT /medications/{id}` 

- Deben crear filas en las hojas correspondientes y, si el payload incluye `assign_to_pets`, crear filas en `routineassignments` o `medicationassignments` con `assigned_at = now` y calcular `next` a partir de `start_of_activity` o `assigned_at`.

6. `GET /medications/{petid}` , `GET /ROUTINES/{petid}`
    - Devuelve lista de rutinas o medicamentos del `user_id`. Esto para mostrar en la pestaña correspondiente de cada mascota en la app.