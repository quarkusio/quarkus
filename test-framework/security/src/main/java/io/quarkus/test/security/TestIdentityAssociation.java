package io.quarkus.test.security;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

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

    SecurityIdentity testIdentity;

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
        if (testIdentity != null) {
            return testIdentity;
        }
        return delegate.getIdentity();
    }
}

@RequestScoped
class DelegateSecurityIdentityAssociation extends SecurityIdentityAssociation {

}
