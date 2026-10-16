package io.quarkus.devjsonrpc.runtime.comms;

public interface JsonRpcResponseWriter {

    void write(String message);

    void close();

    boolean isOpen();

    boolean isClosed();

    Object decorateObject(Object object, MessageType messageType);

}
