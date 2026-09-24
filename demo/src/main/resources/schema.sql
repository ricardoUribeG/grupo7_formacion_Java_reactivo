CREATE TABLE IF NOT EXISTS vehiculo (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(16) NOT NULL UNIQUE,
    ciudad VARCHAR(64) NOT NULL,
    cupo_kg INTEGER NOT NULL CHECK (cupo_kg >= 0),
    cupo_kg_original INTEGER NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS despacho (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    ciudad VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128),
    estado VARCHAR(20) NOT NULL,
    tarifa DECIMAL(12, 2),
    minutos_entrega INTEGER,
    score_riesgo INTEGER,
    total_kg DECIMAL(12, 2),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_en TIMESTAMPTZ,
    traza_id VARCHAR(64)
    );
CREATE UNIQUE INDEX IF NOT EXISTS ux_despacho_idem ON despacho (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_despacho_estado_expira ON despacho (estado, expira_en);

CREATE TABLE IF NOT EXISTS paquete (
    id BIGSERIAL PRIMARY KEY,
    despacho_id BIGINT NOT NULL REFERENCES despacho(id) ON DELETE CASCADE,
    vehiculo_id BIGINT NOT NULL,
    peso_kg DECIMAL(10, 2) NOT NULL
    );
CREATE INDEX IF NOT EXISTS ix_paquete_despacho ON paquete (despacho_id);

-- Datos semilla mínimos para poder probar la demo sin cargar el bulk primero
INSERT INTO vehiculo (placa, ciudad, cupo_kg, cupo_kg_original)
VALUES ('ABC123', 'BOG', 500, 500),
       ('XYZ987', 'MDE', 300, 300),
       ('QWE111', 'BOG', 800, 800)
    ON CONFLICT (placa) DO NOTHING;