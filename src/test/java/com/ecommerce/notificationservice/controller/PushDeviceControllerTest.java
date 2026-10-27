package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.TestTokens;
import com.ecommerce.notificationservice.entity.PushDevice;
import com.ecommerce.notificationservice.repository.PushDeviceRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Goes through the real security chain, real JSON binding and the real error handler. */
@SpringBootTest
@AutoConfigureMockMvc
class PushDeviceControllerTest {

    private static final String URL = "/api/v1/notification/push-devices";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PushDeviceRepository devices;

    @BeforeEach
    void clean() {
        devices.deleteAll();
    }

    private Cookie login(long userId) {
        return new Cookie("access_token", TestTokens.token(userId, "ROLE_USER", 60_000));
    }

    @Test
    void savesTheTokenOfTheLoggedInUser() throws Exception {
        mockMvc.perform(post(URL).cookie(login(7))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\"}"))
                .andExpect(status().isCreated());

        assertThat(devices.findByUserId(7L)).extracting(PushDevice::getToken).containsExactly("abc123");
    }

    @Test
    void sendingTheSameTokenTwiceKeepsOneRow() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post(URL).cookie(login(7))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"abc123\"}"))
                    .andExpect(status().isCreated());
        }

        assertThat(devices.count()).isEqualTo(1);
    }

    @Test
    void aTokenMovesToTheNewAccountOnTheSameBrowser() throws Exception {
        devices.save(new PushDevice(7L, "abc123"));

        mockMvc.perform(post(URL).cookie(login(8))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\"}"))
                .andExpect(status().isCreated());

        assertThat(devices.findByToken("abc123")).get().extracting(PushDevice::getUserId).isEqualTo(8L);
    }

    @Test
    void blankTokenGivesTheSharedValidationError() throws Exception {
        mockMvc.perform(post(URL).cookie(login(7))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(URL))
                .andExpect(jsonPath("$.details[0]").value("token: must not be blank"));
    }

    @Test
    void removesOnlyYourOwnToken() throws Exception {
        devices.save(new PushDevice(7L, "abc123"));

        mockMvc.perform(delete(URL + "/abc123").cookie(login(8))).andExpect(status().isNoContent());
        assertThat(devices.count()).isEqualTo(1);

        mockMvc.perform(delete(URL + "/abc123").cookie(login(7))).andExpect(status().isNoContent());
        assertThat(devices.count()).isZero();
    }

    @Test
    void noCookieGives401WithTheReason() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"abc\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_MISSING"))
                .andExpect(jsonPath("$.refreshable").value(false));
    }

    @Test
    void expiredTokenSaysTheUiMayRefresh() throws Exception {
        Cookie expired = new Cookie("access_token", TestTokens.token(7, "ROLE_USER", -1_000));

        mockMvc.perform(post(URL).cookie(expired)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"abc\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.refreshable").value(true));
    }

    @Test
    void forgedTokenIsInvalid() throws Exception {
        Cookie forged = new Cookie("access_token",
                TestTokens.token("another-secret-another-secret-another-secret-1", 7, "ROLE_ADMIN", 60_000));

        mockMvc.perform(post(URL).cookie(forged)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"abc\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_INVALID"));
    }

    @Test
    void healthCheckNeedsNoLoginAndEveryResponseCarriesATraceId() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "abc-123"));
    }

    @Test
    void aBadTraceIdIsReplaced() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "bad id\nfake log line"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }
}
