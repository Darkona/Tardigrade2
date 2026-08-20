package com.darkona.tardigrade.configuration;

/** Raised when configuration.yml exists but cannot be parsed. */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
