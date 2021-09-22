package ilove.quark.us

import java.io.IOException
import javax.enterprise.context.ApplicationScoped
import javax.websocket.*
import javax.websocket.server.PathParam
import javax.websocket.server.ServerEndpoint

@ServerEndpoint("/start-websocket/{name}")
@ApplicationScoped
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
    fun onMessage(message: String, @PathParam("username") name: String) {
        println("onMessage> $name: $message")
    }

    /**
     * Send a message to a remote websocket session
     *
     * @param session the websocket session
     * @param message the message to send
     * @throws IOException              if there is a communication error sending the message object.
     * @throws EncodeException          if there was a problem encoding the message.
     */
    @Throws(EncodeException::class, IOException::class)
    fun sendMessage(session: Session, message: String) {
        session.basicRemote.sendObject(message)
    }
}
