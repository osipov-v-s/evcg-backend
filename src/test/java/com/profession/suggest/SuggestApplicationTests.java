package com.profession.suggest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires an isolated PostgreSQL test database; production database access is forbidden")
class SuggestApplicationTests {

	@Test
	void contextLoads() {
	}

}
