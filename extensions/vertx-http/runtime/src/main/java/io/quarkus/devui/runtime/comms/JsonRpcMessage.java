package io.quarkus.devui.runtime.comms;

/**
 * Allows JSON RPC methods to response with more finer grade message types
 *
 * @param <T> The type of the response object
 */
public class JsonRpcMessage<T> {
    private T response;
    private MessageType messageType;
    private boolean alreadySerialized = false;

    public JsonRpcMessage() {
    }

    public JsonRpcMessage(T response, MessageType messageType) {
        this.response = response;
        this.messageType = messageType;
    }

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public boolean isAlreadySerialized() {
        return alreadySerialized;
    }

    public void setAlreadySerialized(boolean alreadySerialized) {
        this.alreadySerialized = alreadySerialized;
    }
}