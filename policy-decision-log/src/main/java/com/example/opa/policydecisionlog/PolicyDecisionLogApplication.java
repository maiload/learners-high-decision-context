package com.example.opa.policydecisionlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PolicyDecisionLogApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolicyDecisionLogApplication.class, args);
	}

}
