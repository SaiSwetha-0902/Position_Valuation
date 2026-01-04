package com.example.valuation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jms.annotation.EnableJms;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.sushmithashiva04ops.centraleventpublisher.config.OutboxProperties;

@SpringBootApplication(scanBasePackages = {
        "com.example.valuation",   
        "io.github.sushmithashiva04ops.centraleventpublisher"    
})
@EnableConfigurationProperties(OutboxProperties.class)

@EnableScheduling
@EnableJms
public class ValuationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ValuationApplication.class, args);
	}

}