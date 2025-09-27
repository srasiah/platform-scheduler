package com.example.employee.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Entity to capture employee data snapshots for delta comparison.
 * This maintains historical versions of employee records.
 */
@Entity
@Table(name = "employee_snapshot", 
       indexes = {
           @Index(name = "idx_snapshot_batch_employee", columnList = "batch_id, employee_id"),
           @Index(name = "idx_snapshot_employee", columnList = "employee_id"),
           @Index(name = "idx_snapshot_batch", columnList = "batch_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    
    @Column(name = "batch_id", nullable = false)
    private String batchId;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;
    
    // Employee data fields (snapshot of employee at this point in time)
    @Column(name = "name")
    private String name;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "dob")
    @Temporal(TemporalType.DATE)
    private Date dob;
    
    /**
     * Creates a snapshot from an Employee entity
     */
    public static EmployeeSnapshot fromEmployee(Employee employee, String batchId) {
        EmployeeSnapshot snapshot = new EmployeeSnapshot();
        snapshot.setEmployeeId(employee.getId());
        snapshot.setBatchId(batchId);
        snapshot.setSnapshotDate(LocalDateTime.now());
        snapshot.setName(employee.getName());
        snapshot.setAge(employee.getAge());
        snapshot.setStatus(employee.getStatus());
        snapshot.setDob(employee.getDob());
        return snapshot;
    }
}