package com.grimgate.grimgate_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class GrimgateBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrimgateBackendApplication.class, args);
	}

}
