package com.example.employee.repo;

import com.example.employee.entity.EmployeeDelta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EmployeeDeltaRepository extends JpaRepository<EmployeeDelta, Long> {
    
    /**
     * Find all deltas for a specific batch
     */
    List<EmployeeDelta> findByBatchId(String batchId);
    
    /**
     * Find deltas by type for a specific batch
     */
    List<EmployeeDelta> findByBatchIdAndDeltaType(String batchId, EmployeeDelta.DeltaType deltaType);
    
    /**
     * Find all deltas for a specific employee
     */
    List<EmployeeDelta> findByEmployeeIdOrderByDetectedDateDesc(Long employeeId);
    
    /**
     * Find deltas detected within a date range
     */
    List<EmployeeDelta> findByDetectedDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get count of deltas by type for a specific batch
     */
    @Query("SELECT d.deltaType, COUNT(d) FROM EmployeeDelta d WHERE d.batchId = :batchId GROUP BY d.deltaType")
    List<Object[]> countDeltasByTypeForBatch(@Param("batchId") String batchId);
    
    /**
     * Find all NEW employees for a batch
     */
    @Query("SELECT d FROM EmployeeDelta d WHERE d.batchId = :batchId AND d.deltaType = 'NEW'")
    List<EmployeeDelta> findNewEmployeesForBatch(@Param("batchId") String batchId);
    
    /**
     * Find all DELETED employees for a batch
     */
    @Query("SELECT d FROM EmployeeDelta d WHERE d.batchId = :batchId AND d.deltaType = 'DELETED'")
    List<EmployeeDelta> findDeletedEmployeesForBatch(@Param("batchId") String batchId);
    
    /**
     * Find all UPDATED employees for a batch
     */
    @Query("SELECT d FROM EmployeeDelta d WHERE d.batchId = :batchId AND d.deltaType = 'UPDATED'")
    List<EmployeeDelta> findUpdatedEmployeesForBatch(@Param("batchId") String batchId);
}