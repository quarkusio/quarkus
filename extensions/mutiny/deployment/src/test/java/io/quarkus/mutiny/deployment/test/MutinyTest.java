package io.quarkus.mutiny.deployment.test;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MutinyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanUsingMutiny.class));

    @Inject
    BeanUsingMutiny bean;

    @Test
    public void testUni() {
        String s = bean.greeting().await().indefinitely();
        Assertions.assertEquals(s, "hello");
    }

    @Test
    public void testMulti() {
        List<String> list = bean.stream().collectItems().asList().await().indefinitely();
        Assertions.assertEquals(list.get(0), "hello");
        Assertions.assertEquals(list.get(1), "world");
    }

    @ApplicationScoped
    public static class BeanUsingMutiny {

        public Uni<String> greeting() {
            return Uni.createFrom().item(() -> "hello")
                    .emitOn(Infrastructure.getDefaultExecutor());
        }

        public Multi<String> stream() {
            return Multi.createFrom().items("hello", "world")
                    .emitOn(Infrastructure.getDefaultExecutor());
        }

    }
}