package com.example.employee.repo;

import com.example.employee.entity.EmployeeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeSnapshotRepository extends JpaRepository<EmployeeSnapshot, Long> {
    
    /**
     * Find all snapshots for a specific batch
     */
    List<EmployeeSnapshot> findByBatchId(String batchId);
    
    /**
     * Find all snapshots for a specific employee
     */
    List<EmployeeSnapshot> findByEmployeeIdOrderBySnapshotDateDesc(Long employeeId);
    
    /**
     * Find the most recent snapshot for a specific employee
     */
    @Query("SELECT s FROM EmployeeSnapshot s WHERE s.employeeId = :employeeId ORDER BY s.snapshotDate DESC")
    List<EmployeeSnapshot> findMostRecentSnapshotForEmployee(@Param("employeeId") Long employeeId);
    
    /**
     * Find all employee IDs in a specific batch
     */
    @Query("SELECT DISTINCT s.employeeId FROM EmployeeSnapshot s WHERE s.batchId = :batchId")
    List<Long> findEmployeeIdsByBatchId(@Param("batchId") String batchId);
    
    /**
     * Find all snapshots by batch ordered by employee ID
     */
    List<EmployeeSnapshot> findByBatchIdOrderByEmployeeId(String batchId);
}