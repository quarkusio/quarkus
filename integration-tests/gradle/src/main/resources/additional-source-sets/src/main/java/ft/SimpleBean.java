package ft;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimpleBean {

    public String hello() {
        return "hello";
    }
}
