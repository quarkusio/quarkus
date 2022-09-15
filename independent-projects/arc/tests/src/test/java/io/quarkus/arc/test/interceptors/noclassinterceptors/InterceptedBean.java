package io.quarkus.arc.test.interceptors.noclassinterceptors;

import io.quarkus.arc.NoClassInterceptors;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ClassLevel
public class InterceptedBean extends SuperclassWithInterceptor implements Superinterface {
    @MethodLevel
    @NoClassInterceptors
    public InterceptedBean() {
        super();
    }

    public void classLevel() {
    }

    @MethodLevel
    public void classLevelAndMethodLevel() {
    }

    @NoClassInterceptors
    @MethodLevel
    public void methodLevelOnly() {
    }

    @NoClassInterceptors
    public void noInterceptors() {
    }
}
