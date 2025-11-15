package io.quarkus.devui.runtime.comms;

public interface JsonRpcResponseWriter {

    void write(String message);

    void close();

    boolean isOpen();

    boolean isClosed();

    Object decorateObject(Object object, MessageType messageType);

}