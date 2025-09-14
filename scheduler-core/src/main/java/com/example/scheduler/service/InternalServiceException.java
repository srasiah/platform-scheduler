// scheduler/src/main/java/com/example/scheduler/service/InternalServiceException.java
package com.example.scheduler.service;

public class InternalServiceException extends RuntimeException {
    public InternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

