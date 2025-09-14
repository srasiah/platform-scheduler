package com.example.scheduler.jobs;

import com.example.employee.service.EmployeeIngestService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@DisallowConcurrentExecution
public class EmployeeCsvIngestJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvIngestJob.class);
    @Autowired
    private EmployeeIngestService employeeIngestService;

    @Override
    public void execute(JobExecutionContext context) {
        employeeIngestService.ingestFromDirectory();
    }
}
