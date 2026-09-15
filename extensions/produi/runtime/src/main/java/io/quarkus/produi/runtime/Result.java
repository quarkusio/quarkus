package io.quarkus.produi.runtime;

public class Result {
    public final String messageType;
    public final Object object;

    public Result(String messageType, Object object) {
        this.messageType = messageType;
        this.object = object;
    }
}
