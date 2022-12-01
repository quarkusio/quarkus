package ilove.quark.us.funqygooglecloudfunctions;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {
    private String greeting = "Hello";
    private String punctuation = "!";

    public void setGreeting(String greet) {
        greeting = greet;
    }

    public void setPunctuation(String punctuation) {
        this.punctuation = punctuation;
    }

    public String hello(String val) {
        return greeting + " " + val + punctuation;
    }
}
