package io.quarkus.test.security;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.smallrye.mutiny.Uni;

@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
public class TestIdentityAssociation extends SecurityIdentityAssociation {

    @PostConstruct
    public void check() {
        if (LaunchMode.current() != LaunchMode.TEST) {
            //paranoid check
            throw new RuntimeException("TestAuthController can only be used in tests");
        }
    }

    volatile SecurityIdentity testIdentity;

    /**
     * A request scoped delegate that allows the system to function as normal when
     * the user has not been explicitly overridden
     */
    @Inject
    DelegateSecurityIdentityAssociation delegate;

    public SecurityIdentity getTestIdentity() {
        return testIdentity;
    }

    public TestIdentityAssociation setTestIdentity(SecurityIdentity testIdentity) {
        this.testIdentity = testIdentity;
        return this;
    }

    @Override
    public void setIdentity(SecurityIdentity identity) {
        delegate.setIdentity(identity);
    }

    @Override
    public void setIdentity(Uni<SecurityIdentity> identity) {
        delegate.setIdentity(identity);
    }

    @Override
    public Uni<SecurityIdentity> getDeferredIdentity() {
        if (testIdentity != null) {
            return Uni.createFrom().item(testIdentity);
        }
        return delegate.getDeferredIdentity();
    }

    @Override
    public SecurityIdentity getIdentity() {
        //we check the underlying identity first
        //in most cases this will have been set by the TestHttpAuthenticationMechanism
        //this means that all the usual auth process will run, including augmentors and
        //the identity ends up in the routing context
        SecurityIdentity underlying = delegate.getIdentity();
        if (underlying.isAnonymous()) {
            if (testIdentity != null) {
                return testIdentity;
            }
        }
        return delegate.getIdentity();
    }
}

@RequestScoped
class DelegateSecurityIdentityAssociation extends SecurityIdentityAssociation {

}
