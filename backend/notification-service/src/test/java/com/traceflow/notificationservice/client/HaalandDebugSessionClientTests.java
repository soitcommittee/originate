package com.traceflow.notificationservice.client;

import com.traceflow.notificationservice.dto.DebugSessionRequest;
import com.traceflow.notificationservice.exception.AlertConfigurationException;
import com.traceflow.notificationservice.exception.AlertDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HaalandDebugSessionClientTests {
    private MockRestServiceServer server;
    private HaalandDebugSessionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HaalandDebugSessionClient(builder.build(), "https://haaland.invalid/", "secret-token");
    }

    @Test
    void sendsAuthenticatedDebugSessionRequest() {
        server.expect(once(), requestTo("https://haaland.invalid/api/debug-sessions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"repo_url":"https://github.com/example/repo.git","service_name":"loan-service",\
                        "log_text":"java.lang.IllegalStateException: boom","base_ref":"main"}
                        """))
                .andRespond(withSuccess("""
                        {"reference":"INC-1","incident_id":"id-1","status":"queued"}
                        """, MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.create(request())).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void rejectsMissingConfiguration() {
        var unconfigured = new HaalandDebugSessionClient(RestClient.create(), "", "");

        assertThatThrownBy(() -> unconfigured.create(request()))
                .isInstanceOf(AlertConfigurationException.class);
    }

    @Test
    void wrapsRemoteFailure() {
        server.expect(once(), requestTo("https://haaland.invalid/api/debug-sessions"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.create(request()))
                .isInstanceOf(AlertDeliveryException.class)
                .hasMessageContaining("rejected");
        server.verify();
    }

    private DebugSessionRequest request() {
        return new DebugSessionRequest("https://github.com/example/repo.git", "loan-service",
                "java.lang.IllegalStateException: boom", "main");
    }
}
