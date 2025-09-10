// scheduler/src/main/java/com/example/scheduler/bootstrap/SchedulerStartup.java
package com.example.scheduler.bootstrap;

import lombok.RequiredArgsConstructor;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerStartup {

    private final Scheduler scheduler;
    private static final Logger log = LoggerFactory.getLogger(SchedulerStartup.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            scheduler.start();      // ensure not in standby
            log.info("Quartz started; jobs resumed.");
        } catch (SchedulerException e) {
            log.error("Failed to start Quartz", e);
            throw new IllegalStateException(e);
        }
    }
}
