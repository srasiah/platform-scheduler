package com.example.web;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication(scanBasePackages = "com.example")
@EnableJpaRepositories(basePackages = "com.example.persistence.repo")
@EntityScan(basePackages = "com.example.persistence.entity")
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner quartzInfo(Scheduler scheduler) {
        return args -> {
            var md = scheduler.getMetaData();
            log.info("Quartz JobStore: {}", md.getJobStoreClass().getName());
            log.info("Quartz Scheduler Name: {}", md.getSchedulerName());
            log.info("Quartz Instance Id: {}", md.getSchedulerInstanceId());
        };
    }


    @Bean
    public CommandLineRunner printActiveProfiles(org.springframework.core.env.Environment env) {
        return args -> {
            String[] profiles = env.getActiveProfiles();
            if (profiles.length == 0) {
                log.info("No active Spring profiles.");
            } else {
                log.info("Active Spring profiles: {}", String.join(", ", profiles));
            }

            // Print all property sources and their values for debugging
            if (env instanceof org.springframework.core.env.AbstractEnvironment) {
                org.springframework.core.env.MutablePropertySources sources = ((org.springframework.core.env.AbstractEnvironment) env).getPropertySources();
                log.info("--- Spring Environment Properties (non-sensitive) ---");
                java.util.Set<String> printed = new java.util.HashSet<>();
                for (org.springframework.core.env.PropertySource<?> source : sources) {
                    if (source.getSource() instanceof java.util.Map) {
                        java.util.Map<?,?> map = (java.util.Map<?,?>) source.getSource();
                        for (Object key : map.keySet()) {
                            String k = String.valueOf(key);
                            if (!printed.contains(k) && !k.toLowerCase().contains("password") && !k.toLowerCase().contains("secret")) {
                                String v = env.getProperty(k);
                                log.info("{} = {}", k, v);
                                printed.add(k);
                            }
                        }
                    }
                }
                log.info("--- End of Spring Environment Properties ---");
            }
        };
    }

  
}
