package com.example.employee.service;

import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeDelta;
import com.example.employee.entity.EmployeeIngestBatch;


import java.util.List;

/**
 * Service interface for detecting and managing employee data deltas between CSV ingests.
 */
public interface EmployeeDeltaService {
    
    /**
     * Creates a new ingest batch record to track the current CSV processing session.
     * 
     * @param batchId unique identifier for this ingest batch
     * @param csvFileName name of the CSV file being processed
     * @return the created EmployeeIngestBatch entity
     */
    EmployeeIngestBatch createIngestBatch(String batchId, String csvFileName);
    
    /**
     * Updates the ingest batch with processing results.
     * 
     * @param batchId the batch ID to update
     * @param status the final status (COMPLETED or FAILED)
     * @param totalRecords total number of records processed
     * @param newRecords number of new records added
     * @param updatedRecords number of records updated
     * @param errorMessage error message if failed, null if successful
     */
    void updateIngestBatch(String batchId, EmployeeIngestBatch.IngestStatus status, 
                          Integer totalRecords, Integer newRecords, Integer updatedRecords, 
                          String errorMessage);
    
    /**
     * Creates snapshots of current employee data for delta comparison.
     * 
     * @param employees list of employees to snapshot
     * @param batchId the batch ID for this snapshot
     */
    void createEmployeeSnapshots(List<Employee> employees, String batchId);
    
    /**
     * Detects and records deltas between current batch and the previous batch.
     * 
     * @param currentBatchId the ID of the current batch
     * @return list of detected deltas
     */
    List<EmployeeDelta> detectAndRecordDeltas(String currentBatchId);
    
    /**
     * Gets all deltas for a specific batch.
     * 
     * @param batchId the batch ID
     * @return list of deltas for the batch
     */
    List<EmployeeDelta> getDeltasForBatch(String batchId);
    
    /**
     * Gets deltas of a specific type for a batch.
     * 
     * @param batchId the batch ID
     * @param deltaType the type of delta (NEW, UPDATED, DELETED)
     * @return list of deltas matching the criteria
     */
    List<EmployeeDelta> getDeltasForBatch(String batchId, EmployeeDelta.DeltaType deltaType);
    
    /**
     * Gets the most recent completed ingest batch.
     * 
     * @return the most recent batch, or null if none exists
     */
    EmployeeIngestBatch getMostRecentBatch();
    
    /**
     * Gets summary statistics for a batch's deltas.
     * 
     * @param batchId the batch ID
     * @return delta summary object
     */
    DeltaSummary getDeltaSummary(String batchId);
    
    /**
     * Data transfer object for delta summary statistics.
     */
    class DeltaSummary {
        private final String batchId;
        private final int newEmployees;
        private final int updatedEmployees;
        private final int deletedEmployees;
        private final int totalDeltas;
        
        public DeltaSummary(String batchId, int newEmployees, int updatedEmployees, int deletedEmployees) {
            this.batchId = batchId;
            this.newEmployees = newEmployees;
            this.updatedEmployees = updatedEmployees;
            this.deletedEmployees = deletedEmployees;
            this.totalDeltas = newEmployees + updatedEmployees + deletedEmployees;
        }
        
        // Getters
        public String getBatchId() { return batchId; }
        public int getNewEmployees() { return newEmployees; }
        public int getUpdatedEmployees() { return updatedEmployees; }
        public int getDeletedEmployees() { return deletedEmployees; }
        public int getTotalDeltas() { return totalDeltas; }
    }
}