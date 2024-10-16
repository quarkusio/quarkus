package io.quarkus.devtools.commands.data;

import java.util.Objects;

public class QuarkusCommandOutcome extends ValueMap<QuarkusCommandOutcome> {

    public static QuarkusCommandOutcome success() {
        return new QuarkusCommandOutcome(true, null);
    }

    public static QuarkusCommandOutcome failure(String message) {
        Objects.requireNonNull(message, "Message may not be null in case of a failure");

        return new QuarkusCommandOutcome(false, message);
    }

    private final boolean success;

    private final String message;

    private QuarkusCommandOutcome(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
