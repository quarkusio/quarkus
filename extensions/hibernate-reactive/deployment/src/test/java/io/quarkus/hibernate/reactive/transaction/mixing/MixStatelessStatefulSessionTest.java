package io.quarkus.hibernate.reactive.transaction.mixing;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.transaction.Hero;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class MixStatelessStatefulSessionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Hero.class))
            .withConfigurationResource("application.properties");

    @Inject
    Mutiny.SessionFactory mutinySessionFactory;

    @Test
    @RunOnVertxContext
    @Disabled("WIP")
    public void avoidMixingTransactionalAndWithTransactionTest(UniAsserter asserter) {
        Uni<Void> uni = avoidMixingDifferentSessionTypes();

        asserter.assertFailedWith(() -> uni,
                e -> assertThat(e.getCause())
                        .isInstanceOf(UnsupportedOperationException.class)
                        .hasMessage("Cannot mix Stateful and Stateless sessions"));
    }

    @Transactional
    public Uni<Void> avoidMixingDifferentSessionTypes() {
        Hero heroStateful = new Hero("heroStateful");
        Hero heroStateless = new Hero("heroStateless");

        // TODO this is an advanced scenario and we shoulnd't support this

        return mutinySessionFactory
                .withSession(s -> {
                    return s.merge(heroStateful)
                            .flatMap(h -> s.flush()).onItem().invoke(() -> {
                                System.out.println("+++ First merge done");
                            });
                })
                .flatMap(h1 -> {
                    return mutinySessionFactory.withStatelessSession(
                            s -> s.insert(heroStateless)
                                    .onItem().invoke(() -> System.out.println("++++ Second insert done")));
                });
    }

}
