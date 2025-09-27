package com.example.scheduler.jobs;

import com.example.employee.config.EmployeeCsvExtractProperties;
import com.example.employee.service.EmployeeExtractService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EmployeeCsvExtractJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvExtractJob.class);
    private final EmployeeExtractService employeeExtractService;
    private final EmployeeCsvExtractProperties extractProperties;

    public EmployeeCsvExtractJob(EmployeeExtractService employeeExtractService, EmployeeCsvExtractProperties extractProperties) {
        this.employeeExtractService = employeeExtractService;
        this.extractProperties = extractProperties;
    }

    @Override
    public void execute(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        
        // Check if extract is enabled
        if (!extractProperties.isEnabled()) {
            log.info("Employee CSV extract is disabled. Skipping Employee CSV Extract Job: {} - {}", jobGroup, jobName);
            return;
        }
        
        log.info("Starting Employee CSV Extract Job: {} - {}", jobGroup, jobName);
        employeeExtractService.extractToDirectory();
        log.info("Completed Employee CSV Extract Job: {} - {}", jobGroup, jobName);
    }
}
