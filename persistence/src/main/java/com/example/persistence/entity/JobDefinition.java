package com.example.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "job_definition")
public class JobDefinition {

    @Id
    @Column(length = 40)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String name;

    @Column(name = "grp", nullable = false)
    private String grp;

    @Column(name = "job_type")
    private String jobType;


    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "text")
    private String payload;


    // --- getters/setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGrp() { return grp; }
    public void setGrp(String grp) { this.grp = grp; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
