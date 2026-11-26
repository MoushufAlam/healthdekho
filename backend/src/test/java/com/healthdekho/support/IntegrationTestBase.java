package com.healthdekho.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

// Full app + MockMvc against the healthdekho_test DB (V2 seed data).
// @Transactional rolls every test back, so tests can't affect each other.
// The clock is pinned so date rules (grace period, penalty, age) give the same result on any day.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(IntegrationTestBase.FixedClockConfig.class)
public abstract class IntegrationTestBase {

    public static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    protected static final long AARAV = 1001;
    protected static final long MEERA = 1002;
    protected static final long ROHAN = 1003;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    // JPA writes changes to managed entities at commit; flush first when a test reads them back with plain SQL
    protected void flush() {
        entityManager.flush();
    }

    // pretends the request carries a valid JWT for this customer (signature checks are covered in AuthIntegrationTest)
    protected static RequestPostProcessor as(long customerId) {
        return jwt().jwt(token -> token.subject(String.valueOf(customerId)));
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-26T06:30:00Z"), ZoneId.of("Asia/Kolkata"));
        }
    }
}
