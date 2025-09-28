package com.example.scheduler.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class PrintMessageJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(PrintMessageJob.class);

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dm = context.getMergedJobDataMap();
        String payload = dm.getString("payload");
        log.info("PrintMessageJob: {}", payload);
        // throw new RuntimeException("boom"); // uncomment to test FAILED outcome
    }
}
