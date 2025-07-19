package io.quarkus.devui.runtime.comms;

public interface JsonRpcResponseWriter {
    void write(String message);

    void close();

    boolean isOpen();

    boolean isClosed();

    default void accepted() {
    }

    default boolean canHandle(String jsonRpcMethodName){
        return false;
    }
    
    default String 
}