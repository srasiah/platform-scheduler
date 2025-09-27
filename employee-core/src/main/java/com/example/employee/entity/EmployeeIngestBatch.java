package com.example.employee.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to track employee CSV ingest batches for delta comparison.
 */
@Entity
@Table(name = "employee_ingest_batch")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeIngestBatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "batch_id", unique = true, nullable = false)
    private String batchId;
    
    @Column(name = "ingest_date", nullable = false)
    private LocalDateTime ingestDate;
    
    @Column(name = "csv_file_name")
    private String csvFileName;
    
    @Column(name = "total_records")
    private Integer totalRecords;
    
    @Column(name = "new_records")
    private Integer newRecords;
    
    @Column(name = "updated_records")
    private Integer updatedRecords;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IngestStatus status;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    public enum IngestStatus {
        PROCESSING, COMPLETED, FAILED
    }
}