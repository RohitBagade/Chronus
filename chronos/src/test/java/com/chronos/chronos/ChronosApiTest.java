package com.chronos.chronos;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.repository.UserRepository;
import com.chronos.chronos.scheduler.JobSchedulerService;
import com.chronos.chronos.service.JobExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ChronosApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JobExecutionService execution;
    @Autowired JobRepository jobs;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    // Mock the scheduler so no real Quartz triggers fire during tests (deterministic).
    @MockBean JobSchedulerService scheduler;

    private String json(Object o) throws Exception { return om.writeValueAsString(o); }

    private String signup(String email) throws Exception {
        MvcResult r = mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", "password123"))))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    // ---- auth + web layer ----

    @Test
    void signupLoginAndAuthGuards() throws Exception {
        String token = signup("auth@chronos.test");
        assertThat(token).isNotBlank();

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "auth@chronos.test", "password", "password123"))))
                .andExpect(status().isOk());

        // no token -> 401
        mvc.perform(get("/jobs")).andExpect(status().isUnauthorized());
        // wrong password -> 401
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "auth@chronos.test", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
        // health is public
        mvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void createAndListJob() throws Exception {
        String token = signup("jobs@chronos.test");
        mvc.perform(post("/jobs").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("jobType", "demo", "command", "noop", "scheduleTime", "2030-01-01T10:00:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.maxAttempts").value(3));

        mvc.perform(get("/jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].command").value("noop"));
    }

    // ---- execution engine ----

    private Job newJob(String command, int maxAttempts) {
        User u = users.findByEmail("exec@chronos.test").orElseGet(() ->
                users.save(User.builder().name("Exec").email("exec@chronos.test")
                        .passwordHash(encoder.encode("x")).role("USER").build()));
        return jobs.save(Job.builder().user(u).jobType("t").command(command)
                .scheduleTime(LocalDateTime.now()).status("SCHEDULED")
                .attemptCount(0).maxAttempts(maxAttempts).build());
    }

    private String statusOf(Long id) { return jobs.findById(id).orElseThrow().getStatus(); }

    @Test
    void noopJobSucceeds() {
        Job j = newJob("noop", 3);
        execution.executeJob(j);
        assertThat(statusOf(j.getJobId())).isEqualTo("SUCCESS");
    }

    @Test
    void failingJobRetriesWithBackoff() {
        Job j = newJob("fail", 3);
        execution.executeJob(j);
        assertThat(statusOf(j.getJobId())).isEqualTo("RETRYING");
        assertThat(jobs.findById(j.getJobId()).orElseThrow().getAttemptCount()).isEqualTo(1);
        verify(scheduler).scheduleRetry(eq(j.getJobId()), anyLong());
    }

    @Test
    void failingJobExhaustsAttempts() {
        Job j = newJob("fail", 1); // single attempt -> straight to FAILED, no retry
        execution.executeJob(j);
        assertThat(statusOf(j.getJobId())).isEqualTo("FAILED");
    }

    @Test
    void httpJobRefusesInternalHost() {
        Job j = newJob("http:localhost:9999/admin", 1); // SSRF guard blocks before any network call
        execution.executeJob(j);
        assertThat(statusOf(j.getJobId())).isEqualTo("FAILED");
    }

    @Test
    void shellJobDisabledByDefault() {
        Job j = newJob("shell:echo hi", 1); // gated off -> fails without executing
        execution.executeJob(j);
        assertThat(statusOf(j.getJobId())).isEqualTo("FAILED");
    }
}
