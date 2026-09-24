# Despacho reactivo de envíos

Servicio construido con Java 17, Spring Boot WebFlux, Project Reactor,
Spring Data R2DBC y PostgreSQL 15. Gestiona reservas de cupo, asignación y
confirmación de despachos, servicios externos simulados y streams SSE y
NDJSON.

## Requisitos

- Java 17
- Docker Desktop o Podman
- PowerShell 7 en Windows o una terminal bash/zsh en macOS y Linux

## Levantar el proyecto

Los comandos se ejecutan desde la carpeta `demo`.

### Windows con Docker

```powershell
docker compose up -d
.\gradlew.bat build
.\gradlew.bat bootRun
```

### macOS y Linux con Podman

```bash
podman compose up -d
bash ./gradlew build
bash ./gradlew bootRun
```

La aplicación queda disponible en `http://localhost:8081`. Para detener
PostgreSQL:

```bash
podman compose down
```

## Ejecutar las pruebas

PostgreSQL debe estar activo para la suite completa.

```powershell
# Windows
docker compose up -d
.\gradlew.bat test
```

```bash
# macOS y Linux
podman compose up -d
bash ./gradlew test
```

La suite incluye pruebas unitarias, contratos con `WebTestClient`, tiempo
virtual, fuentes controladas y concurrencia real contra PostgreSQL.

## Prueba rápida

Crear un despacho:

```bash
curl -i -X POST http://localhost:8081/api/despachos \
  -H "Content-Type: application/json" \
  -H "X-Traza-Id: taller-1" \
  -H "Idempotency-Key: K1" \
  -d '{"clienteId":1,"ciudad":"BOG","paquetes":[{"vehiculoId":1,"pesoKg":120}]}'
```

Consultar y confirmar:

```bash
curl http://localhost:8081/api/despachos/1
curl -X POST http://localhost:8081/api/despachos/1/confirm
```

Abrir los streams:

```bash
curl -N http://localhost:8081/api/ops/tablero
curl -N http://localhost:8081/api/despachos/1/events
curl -N http://localhost:8081/api/reports/ciudades/stream
```

Forzar fallos en los servicios simulados:

```bash
curl -X PUT http://localhost:8081/external/simulator \
  -H "Content-Type: application/json" \
  -d '{"fallasTarifa":2,"latenciaRiesgoMs":3000,"scoreRiesgo":95}'

curl -X DELETE http://localhost:8081/external/simulator
```

En PowerShell se debe usar `curl.exe` y guardar los cuerpos JSON o NDJSON en
archivos para evitar problemas con comillas y saltos de línea.

## Endpoints principales

- `POST /api/despachos`
- `GET /api/despachos/{id}`
- `POST /api/despachos/{id}/confirm`
- `GET /api/despachos/{id}/events`
- `GET /api/ops/tablero`
- `GET /api/reports/ciudades`
- `GET /api/reports/ciudades/stream`
- `GET /api/vehiculos`
- `POST /api/vehiculos`
- `POST /api/vehiculos/bulk`
- `GET`, `PUT` y `DELETE /external/simulator`

## Mapa de elementos reactivos

| Elemento reactivo | Archivo y línea | Uso |
|---|---|---|
| `flatMap` | `DespachoService.java:95` | Compone reserva, externos y persistencia |
| `concatMap` | `AsignacionSaga.java:36` | Reserva paquetes en orden y permite compensarlos |
| `Mono.zip` | `DespachoService.java:117` | Ejecuta tarifa, clima y riesgo en paralelo |
| `publishOn` | `DespachoService.java:121` | Ejecuta el cálculo posterior en el scheduler paralelo |
| `retryWhen` | `TransportistaClient.java:63` | Reintenta fallos transitorios de tarifa |
| `timeout` | `TransportistaClient.java:89` | Limita la espera del servicio de riesgo |
| `cache(Duration)` | `TransportistaClient.java:81` | Comparte por diez minutos la ventana de clima |
| `Sinks.many` | `EventBus.java:26-28` | Mantiene los buses internos de eventos |
| `publish().refCount(1)` | `TableroService.java:25-26` | Comparte un único stream caliente del tablero |
| `onBackpressureLatest` | `TableroService.java:30` | Conserva el evento más reciente para clientes lentos |
| `limitRate` | `ReporteService.java:29` | Controla la demanda del reporte por ciudad |
| `onBackpressureDrop` | `ExpiracionJob.java:49` | Descarta ticks si el job anterior sigue activo |
| `distinctUntilChanged` | `DespachoService.java:201` | Evita publicar dos veces consecutivas el mismo estado |
| `Flux.merge` | `DespachoService.java:202` | Fusiona estado actual, eventos y heartbeat |
| `takeUntil` | `DespachoService.java:203` | Cierra el SSE al alcanzar un estado terminal |
| `doOnCancel` y `doFinally` | `DespachoService.java:204-205` | Gestiona y registra el cierre del stream SSE |
| `contextWrite` | `TrazaWebFilter.java:33` | Propaga el `trazaId` mediante Reactor Context |
| `DatabaseClient` y `RETURNING` | `CupoService.java:33-41` | Descuenta cupo mediante una actualización atómica |
| `TransactionalOperator` | `DespachoService.java:146` | Delimita la transacción reactiva de persistencia |
| `buffer` | `VehiculoBulkService.java:27` | Agrupa la carga NDJSON en lotes |
| `StepVerifier.withVirtualTime` | `TransportistaClientTest.java:104` | Verifica el timeout sin esperar tiempo real |
| `TestPublisher` | `TransportistaClientTest.java:113` | Controla la respuesta del servicio de clima |
| `WebTestClient` | `DespachoControllerTest.java:30` | Verifica contratos HTTP y errores |
| Concurrencia reactiva | `CupoConcurrencyIntegrationTest.java:45` | Comprueba que el cupo nunca sea negativo |

## Decisiones técnicas

Las decisiones y alternativas del diseño están documentadas en
`DECISIONES.md`.
