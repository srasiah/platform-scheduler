package com.example.employee.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Entity to track detected changes (deltas) between employee CSV ingests.
 */
@Entity
@Table(name = "employee_delta",
       indexes = {
           @Index(name = "idx_delta_batch_employee", columnList = "batch_id, employee_id"),
           @Index(name = "idx_delta_type", columnList = "delta_type"),
           @Index(name = "idx_delta_batch", columnList = "batch_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDelta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    
    @Column(name = "batch_id", nullable = false)
    private String batchId;
    
    @Column(name = "previous_batch_id")
    private String previousBatchId;
    
    @Column(name = "delta_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeltaType deltaType;
    
    @Column(name = "detected_date", nullable = false)
    private LocalDateTime detectedDate;
    
    // Previous values (for UPDATED and DELETED records)
    @Column(name = "previous_name")
    private String previousName;
    
    @Column(name = "previous_age")
    private Integer previousAge;
    
    @Column(name = "previous_status")
    private String previousStatus;
    
    @Column(name = "previous_dob")
    @Temporal(TemporalType.DATE)
    private Date previousDob;
    
    // Current values (for NEW and UPDATED records)
    @Column(name = "current_name")
    private String currentName;
    
    @Column(name = "current_age")
    private Integer currentAge;
    
    @Column(name = "current_status")
    private String currentStatus;
    
    @Column(name = "current_dob")
    @Temporal(TemporalType.DATE)
    private Date currentDob;
    
    // Detailed change information
    @Column(name = "changed_fields", length = 500)
    private String changedFields; // JSON or comma-separated list of changed field names
    
    @Column(name = "change_summary", length = 1000)
    private String changeSummary; // Human-readable summary of changes
    
    public enum DeltaType {
        NEW,      // Employee added in this batch
        UPDATED,  // Employee data changed from previous batch  
        DELETED   // Employee was in previous batch but not in current batch
    }
}