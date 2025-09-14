package com.example.scheduler.jobs;

import com.example.employee.service.EmployeeExtractService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class EmployeeCsvExtractJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvExtractJob.class);
    @Autowired
    private EmployeeExtractService employeeExtractService;

    @Override
    public void execute(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        log.info("Starting Employee CSV Extract Job: {} - {}", jobGroup, jobName);
        employeeExtractService.extractToDirectory();
        log.info("Completed Employee CSV Extract Job: {} - {}", jobGroup, jobName);
    }
}
