package com.healthdekho;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Smoke test: the whole Spring context starts (needs the local Postgres + DB env vars). */
@SpringBootTest
class HealthDekhoApplicationTests {

    @Test
    void contextLoads() {
    }
}
