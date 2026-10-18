package org.jboss.resteasy.reactive.client.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import wiremock.com.fasterxml.jackson.core.JsonProcessingException;
import wiremock.com.fasterxml.jackson.core.type.TypeReference;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;

class ClientResponseCompleteRestHandlerTest {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Client httpClient = ClientBuilder.newBuilder().build();

    @BeforeAll
    static void start() {
        wireMockServer.stubFor(get(urlPathEqualTo("/get-400"))
                .willReturn(aResponse().withStatus(400).withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .withBody("{\"error\": true, \"message\": \"Invalid parameter\"}")));
        wireMockServer.stubFor(get(urlPathEqualTo("/get-500"))
                .willReturn(aResponse().withStatus(500).withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .withBody("{\"error\": true, \"message\": \"Unexpected error.\"}")));
        wireMockServer.start();
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    private String generateURL(String path) {
        return UriBuilder.fromUri("http://localhost:" + MOCK_SERVER_PORT).path(path).build().toString();
    }

    @Test
    void testResponseOnClientError() throws Exception {
        try {
            httpClient.target(generateURL("/get-400")).request().get(String.class);
            fail("Should have thrown an exception");
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            checkResponse(response);
            Map<String, Object> parsedResponseContent = readAndParseResponse(response);
            assertEquals("Invalid parameter", parsedResponseContent.get("message"));
        }

        // Test when we manually handle the response.
        try (Response response = httpClient.target(generateURL("/get-400")).request().get()) {
            checkResponse(response);
            Map<String, Object> parsedResponseContent = readAndParseResponse(response);
            assertEquals("Invalid parameter", parsedResponseContent.get("message"));
        }
    }

    @Test
    void testResponseOnServerError() throws Exception {
        try {
            httpClient.target(generateURL("/get-500")).request().get(String.class);
            fail("Should have thrown an exception");
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            checkResponse(response);
            Map<String, Object> parsedResponseContent = readAndParseResponse(response);
            assertEquals("Unexpected error.", parsedResponseContent.get("message"));
        }

        // Test when we manually handle the response.
        try (Response response = httpClient.target(generateURL("/get-500")).request().get()) {
            checkResponse(response);
            Map<String, Object> parsedResponseContent = readAndParseResponse(response);
            assertEquals("Unexpected error.", parsedResponseContent.get("message"));
        }
    }

    private void checkResponse(Response response) {
        assertNotNull(response);
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        assertTrue(response.hasEntity());
        response.bufferEntity();
    }

    private Map<String, Object> readAndParseResponse(Response response) throws JsonProcessingException {
        String responseContent = response.readEntity(String.class);
        return objectMapper.readValue(responseContent,
                new TypeReference<>() {
                });
    }

}
