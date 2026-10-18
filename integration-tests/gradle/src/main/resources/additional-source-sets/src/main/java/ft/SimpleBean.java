package ft;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimpleBean {

    public String hello() {
        return "hello";
    }
}
