package io.quarkus.devtools.commands.data;

import java.util.Objects;

public class QuarkusCommandOutcome<T> extends ValueMap<QuarkusCommandOutcome<T>> {

    public static <T> QuarkusCommandOutcome<T> success() {
        return new QuarkusCommandOutcome<>(true, null, null);
    }

    public static <T> QuarkusCommandOutcome<T> success(T result) {
        return new QuarkusCommandOutcome<>(true, null, result);
    }

    public static QuarkusCommandOutcome<Void> failure(String message) {
        Objects.requireNonNull(message, "Message may not be null in case of a failure");

        return new QuarkusCommandOutcome(false, message, null);
    }

    private final boolean success;

    private final String message;

    private final T result;

    private QuarkusCommandOutcome(boolean success, String message, T result) {
        this.success = success;
        this.message = message;
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getResult() {
        return result;
    }
}
