package io.quarkus.test.reload;

import jakarta.annotation.PostConstruct;

//missing scope - the build fails during the first dev mode start
public class SomeBean {

    private String msg;

    public String ping() {
        return msg;
    }

    @PostConstruct
    void init() {
        msg = "pong";
    }

}
