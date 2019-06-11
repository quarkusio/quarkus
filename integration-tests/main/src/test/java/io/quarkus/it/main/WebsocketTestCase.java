/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class WebsocketTestCase {

    @TestHTTPResource("echo")
    URI echoUri;

    @TestHTTPResource("recoder")
    URI recoderUri;

    @Test
    public void websocketTest() throws Exception {

        LinkedBlockingDeque<String> message = new LinkedBlockingDeque<>();
        Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String s) {
                        message.add(s);
                    }
                });
                session.getAsyncRemote().sendText("hello");
            }
        }, ClientEndpointConfig.Builder.create().build(), echoUri);

        try {
            Assertions.assertEquals("hello", message.poll(20, TimeUnit.SECONDS));
        } finally {
            session.close();
        }
    }

    @Test
    public void websocketServerEncodingAndDecodingTest() throws Exception {
        LinkedBlockingDeque<String> message = new LinkedBlockingDeque<>();
        Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String s) {
                        message.add(s);
                    }
                });
                session.getAsyncRemote().sendText("{\"content\":\"message content\"}");
            }
        }, ClientEndpointConfig.Builder.create().build(), recoderUri);

        try {
            Assertions.assertEquals("{\"content\":\"[recoded]message content\"}", message.poll(20, TimeUnit.SECONDS));
        } finally {
            session.close();
        }
    }

    /**
     * In this test:
     * <ul>
     * <li>the test sends a request to {@link io.quarkus.it.websocket.ClientCodingResource}</li>
     * <li>the resource constructs a client that uses an encoder and a decoder and talks with EchoSocket</li>
     * <li>echo service sends its input back to the resource</li>
     * <li>the resource sends the result to the test</li>
     * </ul>
     */
    @Test
    public void websocketClientEncodingAndDecodingTest() {
        String echoUriWsUri = echoUri.toString().replaceFirst("http", "ws");
        System.out.println("echo uri: " + echoUriWsUri); // mstod remove
        RestAssured
                .given()
                .queryParam("echoServerUri", echoUriWsUri)
                .when()
                .get("/ws-client-coding-test")
                .then()
                // "initial-data" is sent from the ClientCodingResource to the EchoService
                // encoder and decoder prepend it with the [encoded] and [decoded] strings.
                .body(is("[decoded][encoded]initial data"));
    }
}
