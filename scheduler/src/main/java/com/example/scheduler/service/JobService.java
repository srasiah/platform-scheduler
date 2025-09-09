package com.example.scheduler.service;

import com.example.common.dto.JobHistoryResponse;
import com.example.common.dto.JobResponse;
import com.example.common.dto.RescheduleRequest;
import com.example.common.dto.ScheduleRequest;
import com.example.persistence.entity.JobDefinition;
import com.example.persistence.entity.JobExecution;
import com.example.persistence.repo.JobDefinitionRepo;
import com.example.persistence.repo.JobExecutionRepo;
import com.example.scheduler.jobs.PrintMessageJob;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.common.dto.JobDetailsResponse;
import org.quartz.CronTrigger;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JobService {
    private final Scheduler scheduler;
    private final JobDefinitionRepo jobDefRepo;
    private final JobExecutionRepo jobExecRepo;

    public JobService(Scheduler scheduler, JobDefinitionRepo jobDefRepo, JobExecutionRepo jobExecRepo) {
        this.scheduler = scheduler;
        this.jobDefRepo = jobDefRepo;
        this.jobExecRepo = jobExecRepo;
    }

    @Transactional
    public JobResponse schedule(ScheduleRequest req) {
        try {
            JobDefinition def = jobDefRepo.findByNameAndGrp(req.name(), req.group())
                    .orElseGet(() -> {
                        JobDefinition d = new JobDefinition();
                        d.setName(req.name());
                        d.setGrp(req.group());
                        d.setJobType(req.jobType());
                        d.setPayload(req.payload());
                        return d;
                    });
            jobDefRepo.save(def);

            JobDetail detail = JobBuilder.newJob(resolveJobClass(req.jobType()))
                    .withIdentity(req.name(), req.group())
                    .usingJobData(new JobDataMap(Map.of("payload", req.payload())))
                    .storeDurably(true)
                    .build();

            Trigger trigger = buildTrigger(req, detail.getKey());

            if (!scheduler.checkExists(detail.getKey())) {
                scheduler.addJob(detail, true);
            }
            scheduler.scheduleJob(trigger);

            return new JobResponse(def.getId(), req.name(), req.group(), "SCHEDULED");
        } catch (IllegalArgumentException e) {
            // e.g. invalid cron or missing fields
            throw new IllegalArgumentException("Invalid schedule request: " + e.getMessage(), e);
        } catch (SchedulerException e) {
            throw new InternalServiceException(
                    "Failed to schedule job %s/%s".formatted(req.group(), req.name()), e);
        }
    }

    public JobResponse reschedule(String name, String group, RescheduleRequest req) {
        TriggerKey tk = TriggerKey.triggerKey(name + "-trigger", group);
        try {
            Trigger existing = scheduler.getTrigger(tk);
            if (existing == null) {
                throw new NotFoundException("Trigger not found: %s/%s".formatted(group, name));
            }
            Trigger newTrigger = buildTriggerForReschedule(name, group, req);
            scheduler.rescheduleJob(tk, newTrigger);
            return new JobResponse(null, name, group, "RESCHEDULED");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid reschedule request: " + e.getMessage(), e);
        } catch (SchedulerException e) {
            throw new InternalServiceException(
                    "Failed to reschedule job %s/%s".formatted(group, name), e);
        }
    }

    public JobResponse stop(String name, String group) {
        JobKey key = JobKey.jobKey(name, group);
        try {
            if (!scheduler.checkExists(key)) {
                throw new NotFoundException("Job not found: %s/%s".formatted(group, name));
            }
            scheduler.pauseJob(key);
            return new JobResponse(null, name, group, "PAUSED");
        } catch (SchedulerException e) {
            throw new InternalServiceException("Failed to pause job %s/%s".formatted(group, name), e);
        }
    }

    public JobResponse resume(String name, String group) {
        JobKey key = JobKey.jobKey(name, group);
        try {
            if (!scheduler.checkExists(key)) {
                throw new NotFoundException("Job not found: %s/%s".formatted(group, name));
            }
            scheduler.resumeJob(key);
            return new JobResponse(null, name, group, "RESUMED");
        } catch (SchedulerException e) {
            throw new InternalServiceException("Failed to resume job %s/%s".formatted(group, name), e);
        }
    }

    public JobDetailsResponse details(String name, String group) throws SchedulerException {
        var key = JobKey.jobKey(name, group);
        var triggers = scheduler.getTriggersOfJob(key);

        String cron = null;
        String nextFireIso = null;

        if (!triggers.isEmpty()) {
            var t = triggers.get(0);
            var next = t.getNextFireTime();
            if (next != null) nextFireIso = next.toInstant().toString();
            if (t instanceof CronTrigger ct) cron = ct.getCronExpression();
        }

        var id = jobDefRepo.findByNameAndGrp(name, group).map(JobDefinition::getId).orElse(null);
        return new JobDetailsResponse(id, name, group, cron, nextFireIso);
    }

    // ---- history recording & retrieval ----

    public void recordStart(String jobId) {
        JobExecution ex = new JobExecution();
        ex.setJobId(jobId);
        ex.setStartedAt(OffsetDateTime.now());
        jobExecRepo.save(ex);
    }

    public void recordFinish(Long execId, String outcome, String message) {
        jobExecRepo.findById(execId).ifPresent(ex -> {
            ex.setFinishedAt(OffsetDateTime.now());
            ex.setOutcome(outcome);
            ex.setMessage(message);
            jobExecRepo.save(ex);
        });
    }

    public List<JobHistoryResponse> history(String jobId) {
        return jobExecRepo.findByJobIdOrderByStartedAtDesc(jobId).stream()
                .map(ex -> new JobHistoryResponse(
                        jobId,
                        ex.getStartedAt(),
                        ex.getFinishedAt(),
                        ex.getOutcome(),
                        ex.getMessage()
                ))
                .collect(Collectors.toList());
    }

    public List<JobHistoryResponse> historyByNameGroup(String name, String group) {
        String jobId = jobDefRepo.findByNameAndGrp(name, group)
                .map(JobDefinition::getId)
                .orElseThrow(() -> new NotFoundException("Job not found: %s/%s".formatted(group, name)));
        return history(jobId);
    }

    public java.util.List<String> supportedTypes() {
        // Keep this in sync with resolveJobClass(...)
        return Arrays.asList(
                "PRINT_MESSAGE"
        );
    }

    public java.util.List<Map<String, String>> listJobsSimple() {
        return jobDefRepo.findAll().stream()
                .map(d -> Map.of(
                        "id", d.getId(),
                        "name", d.getName(),
                        "group", d.getGrp()
                ))
                .collect(Collectors.toList());
    }

    // ---- helpers ----

    private Class<? extends Job> resolveJobClass(String jobType) {
        return switch (jobType) {
            case "PRINT_MESSAGE" -> PrintMessageJob.class;
            default -> throw new IllegalArgumentException("Unknown jobType: " + jobType);
        };
    }

    private Trigger buildTrigger(ScheduleRequest req, JobKey jobKey) {
        TriggerBuilder<Trigger> tb = TriggerBuilder.newTrigger()
                .withIdentity(jobKey.getName() + "-trigger", jobKey.getGroup())
                .forJob(jobKey);

        if (req.cron() != null && !req.cron().isBlank()) {
            return tb.withSchedule(CronScheduleBuilder.cronSchedule(req.cron())).build();
        } else if (req.fixedRateMs() != null) {
            return tb.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(req.fixedRateMs())
                    .repeatForever()).build();
        } else if (req.fixedDelayMs() != null) {
            // Quartz doesn't have fixed-delay; approximate with fixed-rate.
            return tb.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(req.fixedDelayMs())
                    .repeatForever()).build();
        }
        throw new IllegalArgumentException("Provide cron or fixedRateMs/fixedDelayMs");
    }

    private Trigger buildTriggerForReschedule(String name, String group, RescheduleRequest req) {
        TriggerBuilder<Trigger> tb = TriggerBuilder.newTrigger()
                .withIdentity(name + "-trigger", group)
                .forJob(JobKey.jobKey(name, group));

        if (req.newCron() != null && !req.newCron().isBlank()) {
            return tb.withSchedule(CronScheduleBuilder.cronSchedule(req.newCron())).build();
        } else if (req.newFixedRateMs() != null) {
            return tb.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(req.newFixedRateMs())
                    .repeatForever()).build();
        } else if (req.newFixedDelayMs() != null) {
            return tb.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(req.newFixedDelayMs())
                    .repeatForever()).build();
        }
        throw new IllegalArgumentException("Provide newCron or newFixed*Ms");
    }
}
