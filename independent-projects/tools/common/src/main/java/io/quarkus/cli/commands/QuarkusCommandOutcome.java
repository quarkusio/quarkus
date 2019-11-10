package io.quarkus.cli.commands;

public class QuarkusCommandOutcome extends ValueMap<QuarkusCommandOutcome> {

    public static QuarkusCommandOutcome success() {
        return new QuarkusCommandOutcome(true);
    }

    public static QuarkusCommandOutcome failure() {
        return new QuarkusCommandOutcome(false);
    }

    private final boolean success;

    public QuarkusCommandOutcome(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
