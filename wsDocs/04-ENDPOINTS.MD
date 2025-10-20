## Endpoints / Cloud Functions a desarrollar

1. `POST /routines/{routine_id}/perform`
    - **Body**: `{ pet_id, performed_at (ISO) }`
    - **Acción**:
        - Verificar token Firebase → obtener `user_id`.
        - Buscar `assignment` (row) por `routine_id`+`pet_id`+`user_id`.
        - Actualizar `last_performed_at = performed_at`.
        - Calcular `next = addInterval(performed_at, interval_number, interval_unit)` — donde `interval_*` vienen de la tabla `routines` (template).
        - Escribir `next` en `routineassignments.next_activity`.
        - Insertar fila en `activitylog` (action_type = `PERFORMED`, target_table=`routineassignments`, target_id=assignment_id, timestamp, details).
        - Responder `{ assignment_id, last_performed_at, next_activity }`.
2. `POST /medications/{med_id}/perform`
    - **Body**: `{ pet_id, given_at (ISO) }`
    - **Acción**: igual flujo que acima, usando `medications` para intervalo y escribiendo en `medicationassignments.last_given_at` y `.next_supply`.
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