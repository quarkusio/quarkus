package io.quarkus.dev.console;

public interface InputHandler {

    void handleInput(int[] keys);

    void promptHandler(ConsoleStatus promptHandler);

    interface ConsoleStatus {
        void setPrompt(String prompt);

        void setStatus(String status);
    }
}
