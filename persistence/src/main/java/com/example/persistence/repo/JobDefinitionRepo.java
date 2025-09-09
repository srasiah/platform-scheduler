package com.example.persistence.repo;

import com.example.persistence.entity.JobDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JobDefinitionRepo extends JpaRepository<JobDefinition, String> {
    Optional<JobDefinition> findByNameAndGrp(String name, String grp);
}

