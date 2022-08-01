package io.quarkus.arc.test.interceptors.postConstruct.inherited;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OriginalBean {

    public static int TIMES_INVOKED = 0;

    // package private, not visible from AlternativeBean without reflection access
    @PostConstruct
    void postConstruct() {
        TIMES_INVOKED++;
    }

    public String ping() {
        return OriginalBean.class.getSimpleName();
    }

}
