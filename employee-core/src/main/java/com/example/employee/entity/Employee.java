package com.example.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Setter;


@Entity
@Table(name = "employee")
public class Employee {
    @Setter
    @Id
    private Long id;
    @Setter
    private String name;
    @Setter
    private Integer age;
    @Setter
    private String status;
    @Setter
    @Column(name = "dob")
    @Temporal(TemporalType.DATE)
    private java.util.Date dob;
    @Setter
    @Column(name = "batch_id")
    private String batchId;
    @Column(insertable = false, updatable = false, unique = true, name = "transaction_id")
    private Long transactionId;
    @Setter
    @Column(name = "created_date", updatable = false, insertable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date createdDate;

    // Getters and setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getAge() { return age; }
    public String getStatus() { return status; }
    public java.util.Date getDob() { return dob; }
    public String getBatchId() { return batchId; }
    public Long getTransactionId() { return transactionId; }
    public java.util.Date getCreatedDate() { return createdDate; }
}
