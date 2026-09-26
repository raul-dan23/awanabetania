package com.awanabetania.awanabetania;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Runs against the in-memory H2 profile so the suite does not need a live MySQL
// or the production environment variables.
@SpringBootTest
@ActiveProfiles("test")
class AwanaBetaniaApplicationTests {

	@Test
	void contextLoads() {
	}

}
