package io.quarkus.devui.runtime.js;

/**
 * A UI (JavaScript) specific response that contains some more details
 */
public class Result {
    public final String messageType;
    public final Object object;

    public Result(String messageType, Object object) {
        this.messageType = messageType;
        this.object = object;
    }

    @Override
    public String toString() {
        return "Result{" +
                "messageType='" + messageType + '\'' +
                ", object=" + object +
                '}';
    }
}
