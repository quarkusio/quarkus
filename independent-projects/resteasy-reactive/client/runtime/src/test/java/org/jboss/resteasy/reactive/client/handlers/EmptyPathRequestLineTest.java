package org.jboss.resteasy.reactive.client.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.Test;

public class EmptyPathRequestLineTest {

    @Test
    void emptyPathWithQueryIsSentAsSlash() throws Exception {
        assertEquals("GET /?q=1 HTTP/1.1", requestLineFor("?q=1"));
    }

    @Test
    void emptyPathIsSentAsSlash() throws Exception {
        assertEquals("GET / HTTP/1.1", requestLineFor(""));
    }

    private static String requestLineFor(String suffix) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            CompletableFuture<String> requestLine = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = serverSocket.accept()) {
                    String line = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)).readLine();
                    socket.getOutputStream().write(
                            "HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                    .getBytes(StandardCharsets.US_ASCII));
                    return line;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            try (Client client = ClientBuilder.newClient()) {
                client.target("http://localhost:" + serverSocket.getLocalPort() + suffix).request().get().close();
            }
            return requestLine.get(10, TimeUnit.SECONDS);
        }
    }
}
