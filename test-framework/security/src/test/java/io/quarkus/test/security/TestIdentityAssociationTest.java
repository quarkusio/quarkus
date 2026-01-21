package io.quarkus.test.security;

import static io.quarkus.security.runtime.QuarkusSecurityIdentity.builder;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.IOThreadDetector;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.smallrye.mutiny.Uni;

public class TestIdentityAssociationTest {

    TestIdentityAssociation sut;

    @BeforeEach
    void init() {
        sut = new TestIdentityAssociation(new DelegateSecurityIdentityAssociation());

        BlockingOperationControl.setIoThreadDetector(new IOThreadDetector[0]);
    }

    @Test
    void useDelegateIfTestIdentityIsNull() {
        // create anonymous identity
        SecurityIdentity mockedIdentity = builder().setAnonymous(true).build();
        Uni<SecurityIdentity> mockedIdentityUni = Uni.createFrom().item(mockedIdentity);
        sut.setIdentity(mockedIdentity);
        sut.setIdentity(mockedIdentityUni);

        // reset testIdentity
        sut.setTestIdentity(null);

        // get identity direct + deferred
        SecurityIdentity deferred = sut.getDeferredIdentity().await().indefinitely();
        SecurityIdentity identity = sut.getIdentity();

        // must be the same instance
        assertSame(identity, deferred, "Must be same instance directly and deferred");
        assertSame(mockedIdentity, identity, "Expected delegate. (TestIdentity is null)");
    }

    @Test
    void useTestIdentityIfDelegateIsAnonymous() {
        // create anonymous identity
        SecurityIdentity mockedIdentity = builder().setAnonymous(true).build();
        Uni<SecurityIdentity> mockedIdentityUni = Uni.createFrom().item(mockedIdentity);
        // create test identity
        SecurityIdentity mockedTestIdentity = builder().setPrincipal(new QuarkusPrincipal("test-identity")).build();
        sut.setIdentity(mockedIdentity);
        sut.setIdentity(mockedIdentityUni);

        // reset testIdentity
        sut.setTestIdentity(mockedTestIdentity);

        // get identity direct + deferred
        SecurityIdentity deferred = sut.getDeferredIdentity().await().indefinitely();
        SecurityIdentity identity = sut.getIdentity();

        // must be the same instance
        assertSame(identity, deferred, "Must be same instance directly and deferred");
        assertSame(mockedTestIdentity, identity, "Expected testIdentity. (Delegate is anonymous)");
    }

    @Test
    void useDelegateIfNotAnonymous() {
        // create identity with principal
        SecurityIdentity mockedIdentity = builder().setPrincipal(new QuarkusPrincipal("delegate")).build();
        Uni<SecurityIdentity> mockedIdentityUni = Uni.createFrom().item(mockedIdentity);
        // create test identity
        SecurityIdentity mockedTestIdentity = builder().setPrincipal(new QuarkusPrincipal("test-identity")).build();
        sut.setIdentity(mockedIdentity);
        sut.setIdentity(mockedIdentityUni);

        // reset testIdentity
        sut.setTestIdentity(mockedTestIdentity);

        // get identity direct + deferred
        SecurityIdentity deferred = sut.getDeferredIdentity().await().indefinitely();
        SecurityIdentity identity = sut.getIdentity();

        // must be the same instance
        assertSame(identity, deferred, "Must be same instance directly and deferred");
        assertSame(mockedIdentity, identity, "Expected delegate. (Delegate is not anonymous)");
    }
}
