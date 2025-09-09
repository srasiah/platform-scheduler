// scheduler/src/main/java/com/example/scheduler/config/JobHistoryListener.java
package com.example.scheduler.config;

import com.example.persistence.entity.JobDefinition;
import com.example.persistence.entity.JobExecution;
import com.example.persistence.repo.JobDefinitionRepo;
import com.example.persistence.repo.JobExecutionRepo;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class JobHistoryListener extends JobListenerSupport {
    public static final String EXEC_ID_KEY = "execId";

    private final JobDefinitionRepo jobDefRepo;
    private final JobExecutionRepo jobExecRepo;

    public JobHistoryListener(JobDefinitionRepo jobDefRepo, JobExecutionRepo jobExecRepo) {
        this.jobDefRepo = jobDefRepo;
        this.jobExecRepo = jobExecRepo;
    }

    @Override public String getName() { return "job-history-listener"; }

    @Override
    public void jobToBeExecuted(JobExecutionContext ctx) {
        JobKey key = ctx.getJobDetail().getKey();
        String name = key.getName();
        String group = key.getGroup();

        String jobId = jobDefRepo.findByNameAndGrp(name, group)
                .map(JobDefinition::getId)
                .orElse(null);

        JobExecution ex = new JobExecution();
        ex.setJobId(jobId);
        ex.setStartedAt(OffsetDateTime.now());
        ex = jobExecRepo.save(ex);

        ctx.getMergedJobDataMap().put(EXEC_ID_KEY, ex.getId());
    }

    @Override
    public void jobWasExecuted(JobExecutionContext ctx, JobExecutionException jobException) {
        Object idObj = ctx.getMergedJobDataMap().get(EXEC_ID_KEY);
        if (!(idObj instanceof Number)) return;

        Long execId = ((Number) idObj).longValue();
        jobExecRepo.findById(execId).ifPresent(ex -> {
            ex.setFinishedAt(OffsetDateTime.now());
            if (jobException == null) {
                ex.setOutcome("SUCCESS");
                ex.setMessage("OK");
            } else {
                ex.setOutcome("FAILED");
                ex.setMessage(jobException.getMessage());
            }
            jobExecRepo.save(ex);
        });
    }
}
