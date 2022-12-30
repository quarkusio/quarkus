package io.quarkus.smallrye.reactivemessaging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import io.quarkus.test.QuarkusUnitTest;

@SetEnvironmentVariable(key = "MP_MESSAGING_INCOMING_WAY_IN_TRACING_ENABLED", value = "false")
public class ConnectorConfigSupportsEnvVarOverridesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DumbConnector.class, BeanUsingDummyConnector.class))
            .overrideConfigKey("mp.messaging.incoming.way-in.values", "bonjour")
            .overrideConfigKey("mp.messaging.incoming.way-in.connector", "dummy");

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

        @Incoming("way-in")
        public void consume(String s) {
            list.add(s);
        }

        public List<String> getList() {
            return list;
        }

    }
}
