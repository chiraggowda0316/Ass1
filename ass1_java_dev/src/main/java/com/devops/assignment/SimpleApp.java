package com.devops.assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SimpleApp {

    public static void main(String[] args) {
        SpringApplication.run(SimpleApp.class, args);
    }

    @GetMapping("/")
    public String home() {
        String env = System.getenv("APP_ENV");
        String message = System.getenv("APP_MESSAGE");

        if (env == null) env = "development";
        if (message == null) message = "Welcome to intelliclouds!";

        return String.format("""
            {
              "message": "%s",
              "environment": "%s",
              "javaVersion": "%s",
              "status": "running"
            }
            """, message, env, System.getProperty("java.version"));
    }

    @GetMapping("/health")
    public String health() {
        return """
            {
              "status": "UP",
              "service": "intelliclouds"
            }
            """;
    }

    @GetMapping("/info")
    public String info() {
        return String.format("""
            {
              "app": "intelliclouds",
              "version": "1.0.0",
              "environment": "%s",
              "javaVersion": "%s"
            }
            """, System.getenv("APP_ENV") != null ? System.getenv("APP_ENV") : "development",
                System.getProperty("java.version"));
    }
}