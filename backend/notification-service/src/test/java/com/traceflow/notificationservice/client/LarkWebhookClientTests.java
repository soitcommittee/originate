package com.traceflow.notificationservice.client;

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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LarkWebhookClientTests {
    private MockRestServiceServer server;
    private LarkWebhookClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LarkWebhookClient(builder);
    }

    @Test
    void acceptsSuccessfulLarkResponse() {
        server.expect(once(), requestTo("https://example.invalid/lark-hook"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"msg_type":"text","content":{"text":"Originate test"}}
                        """))
                .andRespond(withSuccess("{\"code\":0,\"msg\":\"success\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.sendText("https://example.invalid/lark-hook", "Originate test"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void rejectsUnsuccessfulLarkResponse() {
        server.expect(once(), requestTo("https://example.invalid/lark-hook"))
                .andRespond(withSuccess("{\"code\":19024,\"msg\":\"key words not found\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.sendText("https://example.invalid/lark-hook", "test"))
                .isInstanceOf(AlertDeliveryException.class)
                .hasMessageContaining("unsuccessful");
        server.verify();
    }
}
