package com.example.scheduler.jobs;

import com.example.employee.config.EmployeeCsvIngestProperties;
import com.example.employee.service.EmployeeIngestService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@DisallowConcurrentExecution
public class EmployeeCsvIngestJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvIngestJob.class);
    private final EmployeeIngestService employeeIngestService;
    private final EmployeeCsvIngestProperties ingestProperties;

    public EmployeeCsvIngestJob(EmployeeIngestService employeeIngestService, EmployeeCsvIngestProperties ingestProperties) {
        this.employeeIngestService = employeeIngestService;
        this.ingestProperties = ingestProperties;
    }

    @Override
    public void execute(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        
        // Check if ingest is enabled
        if (!ingestProperties.isEnabled()) {
            log.info("Employee CSV ingest is disabled. Skipping Employee CSV Ingest Job: {} - {}", jobGroup, jobName);
            return;
        }
        
        log.info("Starting Employee CSV Ingest Job: {} - {}", jobGroup, jobName);
        employeeIngestService.ingestFromDirectory();
        log.info("Completed Employee CSV Ingest Job: {} - {}", jobGroup, jobName);
    }
}
