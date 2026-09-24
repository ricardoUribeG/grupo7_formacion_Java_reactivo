# Decisiones técnicas

- **`concatMap` para reservar y liberar cupo:** se descartó `flatMap` porque la saga necesita conservar el orden de los paquetes y conocer exactamente cuáles debe compensar cuando una reserva falla.
- **`flatMap` para operaciones dependientes:** se usa cuando una operación necesita el resultado de la anterior y no existe un requisito de orden entre varios elementos; se descartó anidar suscripciones porque dificulta propagar errores y cancelaciones.
- **`onBackpressureLatest` en el tablero:** se descartó acumular todos los eventos para cada operador lento porque un tablero necesita mostrar el estado más reciente y no procesar información obsoleta.
- **Tablero hot con `publish().refCount(1)`:** se descartó un publisher cold por cliente porque duplicaría la suscripción y el trabajo aguas arriba; todos los operadores deben observar la misma fuente viva.
- **Transacción reactiva:** comienza en `persistirAsignado` antes de guardar el despacho y sus paquetes, y termina cuando ambas escrituras confirman o revierten; la reserva de cupo queda fuera porque usa una actualización atómica y se compensa mediante la saga.
- **Reserva con `UPDATE ... WHERE ... RETURNING`:** se descartó leer, restar y guardar en Java porque dos solicitudes concurrentes podrían usar el mismo cupo; PostgreSQL valida y descuenta en una sola operación.
- **Resiliencia por servicio externo:** tarifa usa reintento y fallback, clima usa caché y riesgo usa timeout; se descartó una política genérica porque cada dependencia presenta un tipo de fallo distinto.
- **Reactor Context para `trazaId`:** se descartó pasarlo como parámetro por todas las capas para mantener las firmas de negocio limpias y conservar la trazabilidad dentro de la cadena reactiva.
