// scheduler/src/main/java/com/example/scheduler/config/QuartzConfig.java
package com.example.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {

    private final JobHistoryListener historyListener;

    public QuartzConfig(JobHistoryListener historyListener) {
        this.historyListener = historyListener;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);

        // 👇 This registers your history listener for ALL jobs
        factory.setGlobalJobListeners(historyListener);

        // (Optional) If you ever need DI inside your Job classes, set a Spring-aware JobFactory here.
        // factory.setJobFactory(new AutowiringSpringBeanJobFactory(applicationContext.getAutowireCapableBeanFactory()));

        return factory;
    }
}
