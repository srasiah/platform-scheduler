package com.example.persistence.repo;

import com.example.persistence.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobExecutionRepo extends JpaRepository<JobExecution, Long> {
    List<JobExecution> findByJobIdOrderByStartedAtDesc(String jobId);
}

