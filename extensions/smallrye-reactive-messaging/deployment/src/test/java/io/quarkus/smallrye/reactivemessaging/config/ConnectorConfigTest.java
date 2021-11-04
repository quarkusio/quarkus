package io.quarkus.smallrye.reactivemessaging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConnectorConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DumbConnector.class, BeanUsingDummyConnector.class))
            .overrideConfigKey("mp.messaging.incoming.a.values", "bonjour")
            .overrideConfigKey("mp.messaging.incoming.a.connector", "dummy");

    @Inject
    BeanUsingDummyConnector bean;

    @Test
    public void test() {
        await().until(() -> bean.getList().size() == 2);
        assertThat(bean.getList()).containsExactly("bonjour", "BONJOUR");
    }

    @ApplicationScoped
    public static class BeanUsingDummyConnector {

        private List<String> list = new CopyOnWriteArrayList<>();

        @Incoming("a")
        public void consume(String s) {
            list.add(s);
        }

        public List<String> getList() {
            return list;
        }

    }
}
