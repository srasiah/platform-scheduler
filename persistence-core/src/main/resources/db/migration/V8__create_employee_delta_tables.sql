-- V8__create_employee_delta_tables.sql
-- Migration to create tables for tracking employee CSV delta changes

-- Table to track ingest batches
CREATE TABLE employee_ingest_batch (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(255) UNIQUE NOT NULL,
    ingest_date TIMESTAMP NOT NULL,
    csv_file_name VARCHAR(500),
    total_records INTEGER,
    new_records INTEGER,
    updated_records INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    error_message VARCHAR(1000)
);

-- Index for efficient batch lookups
CREATE INDEX idx_ingest_batch_date ON employee_ingest_batch(ingest_date DESC);
CREATE INDEX idx_ingest_batch_status ON employee_ingest_batch(status);

-- Table to store employee data snapshots for each ingest
CREATE TABLE employee_snapshot (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    batch_id VARCHAR(255) NOT NULL,
    snapshot_date TIMESTAMP NOT NULL,
    name VARCHAR(255),
    age INTEGER,
    status VARCHAR(100),
    dob DATE
);

-- Indexes for efficient snapshot queries
CREATE INDEX idx_snapshot_batch_employee ON employee_snapshot(batch_id, employee_id);
CREATE INDEX idx_snapshot_employee ON employee_snapshot(employee_id);
CREATE INDEX idx_snapshot_batch ON employee_snapshot(batch_id);

-- Table to track detected changes (deltas) between batches
CREATE TABLE employee_delta (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    batch_id VARCHAR(255) NOT NULL,
    previous_batch_id VARCHAR(255),
    delta_type VARCHAR(20) NOT NULL, -- NEW, UPDATED, DELETED
    detected_date TIMESTAMP NOT NULL,
    
    -- Previous values (for UPDATED and DELETED records)
    previous_name VARCHAR(255),
    previous_age INTEGER,
    previous_status VARCHAR(100),
    previous_dob DATE,
    
    -- Current values (for NEW and UPDATED records)
    current_name VARCHAR(255),
    current_age INTEGER,
    current_status VARCHAR(100),
    current_dob DATE,
    
    -- Change metadata
    changed_fields VARCHAR(500), -- JSON or comma-separated list of changed fields
    change_summary VARCHAR(1000) -- Human-readable summary
);

-- Indexes for efficient delta queries
CREATE INDEX idx_delta_batch_employee ON employee_delta(batch_id, employee_id);
CREATE INDEX idx_delta_type ON employee_delta(delta_type);
CREATE INDEX idx_delta_batch ON employee_delta(batch_id);
CREATE INDEX idx_delta_employee ON employee_delta(employee_id);
CREATE INDEX idx_delta_date ON employee_delta(detected_date DESC);

-- Add foreign key constraints
ALTER TABLE employee_snapshot ADD CONSTRAINT fk_snapshot_batch 
    FOREIGN KEY (batch_id) REFERENCES employee_ingest_batch(batch_id);
    
ALTER TABLE employee_delta ADD CONSTRAINT fk_delta_batch 
    FOREIGN KEY (batch_id) REFERENCES employee_ingest_batch(batch_id);

-- Add check constraints for valid enum values
ALTER TABLE employee_ingest_batch ADD CONSTRAINT chk_ingest_status 
    CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'));
    
ALTER TABLE employee_delta ADD CONSTRAINT chk_delta_type 
    CHECK (delta_type IN ('NEW', 'UPDATED', 'DELETED'));