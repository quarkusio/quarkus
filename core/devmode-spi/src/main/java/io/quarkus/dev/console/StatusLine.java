package io.quarkus.dev.console;

public interface StatusLine {
    int STATUS = 100;
    int TEST_RESULTS = 200;
    int COMPILE_ERROR = 300;

    void setMessage(String message);

    void close();

}
