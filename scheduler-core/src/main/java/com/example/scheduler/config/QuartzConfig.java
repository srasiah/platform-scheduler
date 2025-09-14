// scheduler/src/main/java/com/example/scheduler/config/QuartzConfig.java
package com.example.scheduler.config;

import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

  private final JobHistoryListener historyListener;

  public QuartzConfig(JobHistoryListener historyListener) {
    this.historyListener = historyListener;
  }

  // ✅ Don’t define a @Bean SchedulerFactoryBean.
  // ✅ Customize the auto-configured one instead.
  @Bean
  public SchedulerFactoryBeanCustomizer quartzCustomizer() {
    return factory -> {
      factory.setOverwriteExistingJobs(true);
      factory.setWaitForJobsToCompleteOnShutdown(true);
      factory.setGlobalJobListeners(historyListener);
      // If you need DI in Job classes, also set a Spring-aware JobFactory here.
      // factory.setJobFactory(autowiringJobFactory);
    };
  }
}
