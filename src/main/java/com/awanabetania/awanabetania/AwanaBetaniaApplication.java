package com.awanabetania.awanabetania;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AwanaBetania Spring Boot application.
 * Bootstraps the application context and starts the embedded server.
 */
@SpringBootApplication
public class AwanaBetaniaApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments passed to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(AwanaBetaniaApplication.class, args);
	}

}
