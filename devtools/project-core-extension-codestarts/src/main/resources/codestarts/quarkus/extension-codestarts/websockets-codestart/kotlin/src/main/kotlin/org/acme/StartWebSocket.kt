package org.acme

import javax.enterprise.context.ApplicationScoped
import javax.websocket.*
import javax.websocket.server.PathParam
import javax.websocket.server.ServerEndpoint

@ApplicationScoped
@ServerEndpoint("/start-websocket/{name}")
class StartWebSocket {

    @OnOpen
    fun onOpen(session: Session?, @PathParam("name") name: String) {
        println("onOpen> $name")
    }

    @OnClose
    fun onClose(session: Session?, @PathParam("name") name: String) {
        println("onClose> $name")
    }

    @OnError
    fun onError(session: Session?, @PathParam("name") name: String, throwable: Throwable) {
        println("onError> $name: $throwable")
    }

    @OnMessage
    fun onMessage(message: String, @PathParam("name") name: String) {
        println("onMessage> $name: $message")
    }
}
