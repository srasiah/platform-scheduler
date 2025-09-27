package com.example.employee.repo;

import com.example.employee.entity.EmployeeIngestBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmployeeIngestBatchRepository extends JpaRepository<EmployeeIngestBatch, Long> {
    
    /**
     * Find batch by batch ID
     */
    Optional<EmployeeIngestBatch> findByBatchId(String batchId);
    
    /**
     * Find the most recent completed batch before the given date
     */
    @Query("SELECT b FROM EmployeeIngestBatch b WHERE b.status = 'COMPLETED' AND b.ingestDate < :beforeDate ORDER BY b.ingestDate DESC")
    List<EmployeeIngestBatch> findMostRecentCompletedBatchBefore(LocalDateTime beforeDate);
    
    /**
     * Find the most recent completed batch
     */
    @Query("SELECT b FROM EmployeeIngestBatch b WHERE b.status = 'COMPLETED' ORDER BY b.ingestDate DESC")
    List<EmployeeIngestBatch> findMostRecentCompletedBatch();
    
    /**
     * Find all batches ordered by ingest date
     */
    List<EmployeeIngestBatch> findAllByOrderByIngestDateDesc();
}