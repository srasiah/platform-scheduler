package com.example.web.controller;

import com.example.employee.entity.EmployeeDelta;
import com.example.employee.entity.EmployeeIngestBatch;
import com.example.employee.service.EmployeeDeltaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for querying employee delta tracking data.
 */
@RestController
@RequestMapping("/api/employee-deltas")
public class EmployeeDeltaController {
    
    private final EmployeeDeltaService deltaService;
    
    public EmployeeDeltaController(EmployeeDeltaService deltaService) {
        this.deltaService = deltaService;
    }
    
    /**
     * Get all deltas for a specific batch.
     */
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<EmployeeDelta>> getDeltasForBatch(@PathVariable String batchId) {
        List<EmployeeDelta> deltas = deltaService.getDeltasForBatch(batchId);
        return ResponseEntity.ok(deltas);
    }
    
    /**
     * Get new employees for a specific batch.
     */
    @GetMapping("/batch/{batchId}/new")
    public ResponseEntity<List<EmployeeDelta>> getNewEmployeesForBatch(@PathVariable String batchId) {
        List<EmployeeDelta> newEmployees = deltaService.getDeltasForBatch(batchId, EmployeeDelta.DeltaType.NEW);
        return ResponseEntity.ok(newEmployees);
    }
    
    /**
     * Get updated employees for a specific batch.
     */
    @GetMapping("/batch/{batchId}/updated")
    public ResponseEntity<List<EmployeeDelta>> getUpdatedEmployeesForBatch(@PathVariable String batchId) {
        List<EmployeeDelta> updatedEmployees = deltaService.getDeltasForBatch(batchId, EmployeeDelta.DeltaType.UPDATED);
        return ResponseEntity.ok(updatedEmployees);
    }
    
    /**
     * Get deleted employees for a specific batch.
     */
    @GetMapping("/batch/{batchId}/deleted")
    public ResponseEntity<List<EmployeeDelta>> getDeletedEmployeesForBatch(@PathVariable String batchId) {
        List<EmployeeDelta> deletedEmployees = deltaService.getDeltasForBatch(batchId, EmployeeDelta.DeltaType.DELETED);
        return ResponseEntity.ok(deletedEmployees);
    }
    
    /**
     * Get delta summary for a specific batch.
     */
    @GetMapping("/batch/{batchId}/summary")
    public ResponseEntity<EmployeeDeltaService.DeltaSummary> getDeltaSummaryForBatch(@PathVariable String batchId) {
        EmployeeDeltaService.DeltaSummary summary = deltaService.getDeltaSummary(batchId);
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get the most recent ingest batch.
     */
    @GetMapping("/batch/latest")
    public ResponseEntity<EmployeeIngestBatch> getMostRecentBatch() {
        EmployeeIngestBatch batch = deltaService.getMostRecentBatch();
        if (batch != null) {
            return ResponseEntity.ok(batch);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get deltas for the most recent batch.
     */
    @GetMapping("/latest")
    public ResponseEntity<List<EmployeeDelta>> getLatestDeltas() {
        EmployeeIngestBatch batch = deltaService.getMostRecentBatch();
        if (batch != null) {
            List<EmployeeDelta> deltas = deltaService.getDeltasForBatch(batch.getBatchId());
            return ResponseEntity.ok(deltas);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get new employees from the most recent batch.
     */
    @GetMapping("/latest/new")
    public ResponseEntity<List<EmployeeDelta>> getLatestNewEmployees() {
        EmployeeIngestBatch batch = deltaService.getMostRecentBatch();
        if (batch != null) {
            List<EmployeeDelta> newEmployees = deltaService.getDeltasForBatch(batch.getBatchId(), EmployeeDelta.DeltaType.NEW);
            return ResponseEntity.ok(newEmployees);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get updated employees from the most recent batch.
     */
    @GetMapping("/latest/updated")
    public ResponseEntity<List<EmployeeDelta>> getLatestUpdatedEmployees() {
        EmployeeIngestBatch batch = deltaService.getMostRecentBatch();
        if (batch != null) {
            List<EmployeeDelta> updatedEmployees = deltaService.getDeltasForBatch(batch.getBatchId(), EmployeeDelta.DeltaType.UPDATED);
            return ResponseEntity.ok(updatedEmployees);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get deleted employees from the most recent batch.
     */
    @GetMapping("/latest/deleted")
    public ResponseEntity<List<EmployeeDelta>> getLatestDeletedEmployees() {
        EmployeeIngestBatch batch = deltaService.getMostRecentBatch();
        if (batch != null) {
            List<EmployeeDelta> deletedEmployees = deltaService.getDeltasForBatch(batch.getBatchId(), EmployeeDelta.DeltaType.DELETED);
            return ResponseEntity.ok(deletedEmployees);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get summary of the most recent batch.
     */
    @GetMapping("/latest/summary")
    public ResponseEntity<EmployeeDeltaService.DeltaSummary> getLatestDeltaSummary() {
        EmployeeIngestBatch batch = deltaService.getMostRecentBatch();
        if (batch != null) {
            EmployeeDeltaService.DeltaSummary summary = deltaService.getDeltaSummary(batch.getBatchId());
            return ResponseEntity.ok(summary);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}