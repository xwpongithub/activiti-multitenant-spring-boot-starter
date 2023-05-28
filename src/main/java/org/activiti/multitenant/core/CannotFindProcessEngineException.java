package org.activiti.multitenant.core;

public class CannotFindProcessEngineException extends RuntimeException{

    public CannotFindProcessEngineException(String message) {
        super(message);
    }

    public CannotFindProcessEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
