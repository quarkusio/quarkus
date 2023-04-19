package org.acme

import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint

@ServerEndpoint("/start-websocket/{name}")
@ApplicationScoped
class StartWebSocket {

    @OnOpen
    void onOpen(Session session, @PathParam("name") String name) {
        println "onOpen> ${name}"
    }

    @OnClose
    void onClose(Session session, @PathParam("name") String name) {
        println "onClose> ${name}"
    }

    @OnError
    void onError(Session session, @PathParam("name") String name, Throwable throwable) {
        println "onError> ${name}: ${throwable}"
    }

    @OnMessage
    void onMessage(String message, @PathParam("name") String name) {
        println "onMessage> ${name}: ${message}"
    }
}
