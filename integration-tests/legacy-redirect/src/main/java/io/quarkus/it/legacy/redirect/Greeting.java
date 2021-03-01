package io.quarkus.it.legacy.redirect;

import java.time.LocalTime;

public class Greeting extends Salutation {

    private String message;
    private LocalTime time;

    public Greeting() {
    }

    public Greeting(String message, LocalTime time) {
        this.message = message;
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    @Override
    public String getType() {
        return "Greet";
    }

}
