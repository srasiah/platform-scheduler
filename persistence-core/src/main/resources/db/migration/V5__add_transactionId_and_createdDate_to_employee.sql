-- V5__add_transactionId_and_createdDate_to_employee.sql
-- Adds transaction_id (sequential) and created_date to employee table

ALTER TABLE employee
ADD COLUMN transaction_id BIGSERIAL UNIQUE,
ADD COLUMN created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

