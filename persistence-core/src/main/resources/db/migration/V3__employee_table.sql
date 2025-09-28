-- V3__employee_table.sql
-- Migration to create the employee table for CSV ingestion

CREATE TABLE IF NOT EXISTS employee (
    id   BIGINT PRIMARY KEY,
    name VARCHAR(255),
    age  INTEGER
);
