// scheduler/src/main/java/com/example/scheduler/service/NotFoundException.java
package com.example.scheduler.service;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

