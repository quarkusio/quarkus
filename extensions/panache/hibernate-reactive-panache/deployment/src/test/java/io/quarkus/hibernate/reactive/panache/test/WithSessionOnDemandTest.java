package io.quarkus.hibernate.reactive.panache.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class WithSessionOnDemandTest {

    @RegisterExtension
    static QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(MrBean.class, MyEntity.class));

    @Inject
    MrBean bean;

    @RunOnVertxContext
    @Test
    public void testSharedSession(UniAsserter asserter) {
        asserter.assertEquals(() -> bean.ping(), "true");
    }

    @Unremovable
    @ApplicationScoped
    static class MrBean {

        @WithSessionOnDemand
        Uni<String> ping() {
            Uni<Session> s1 = Panache.getSession();
            Uni<Session> s2 = Panache.getSession();
            // Test that both unis receive the same session
            return s1.chain(s1Val -> s2.map(s2Val -> "" + (s1Val == s2Val)));
        }

    }
}
