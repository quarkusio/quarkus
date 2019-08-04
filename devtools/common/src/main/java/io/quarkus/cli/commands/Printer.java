package io.quarkus.cli.commands;

public class Printer {
    private static final String OK = "\u2705";
    static final String NOK = "\u274c";
    private static final String NOOP = "\uD83D\uDC4D";

    public void nok(String content) {
        print(NOK + content);
    }

    public void ok(String content) {
        print(OK + content);
    }

    public void noop(String content) {
        print(NOOP + content);
    }

    void print(String message) {
        System.out.println(message);
    }

}
