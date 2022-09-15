package io.quarkus.test.security;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.interceptor.Interceptor;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.spi.runtime.AuthorizationController;

@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
public class TestAuthController extends AuthorizationController {

    @PostConstruct
    public void check() {
        if (LaunchMode.current() != LaunchMode.TEST) {
            //paranoid check
            throw new RuntimeException("TestAuthController can only be used in tests");
        }
    }

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public TestAuthController setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public boolean isAuthorizationEnabled() {
        return enabled;
    }
}
