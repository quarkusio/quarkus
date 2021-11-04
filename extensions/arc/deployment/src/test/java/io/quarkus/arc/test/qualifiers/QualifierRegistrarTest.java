package io.quarkus.arc.test.qualifiers;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class QualifierRegistrarTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NotAQualifier.class, SimpleBean.class, Client.class))
            .addBuildChainCustomizer(b -> {
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new QualifierRegistrarBuildItem(new QualifierRegistrar() {
                            @Override
                            public Map<DotName, Set<String>> getAdditionalQualifiers() {
                                return Collections.singletonMap(DotName.createSimple(NotAQualifier.class.getName()), null);
                            }
                        }));
                    }
                }).produces(QualifierRegistrarBuildItem.class).build();
            });

    @Inject
    Client client;

    @Test
    public void testQualifier() {
        assertTrue(client.instance.isUnsatisfied());
        assertEquals("PONG", client.simpleBean.ping());
    }

    @Dependent
    static class Client {

        @NotAQualifier
        SimpleBean simpleBean;

        @Inject
        Instance<SimpleBean> instance;

    }

    @NotAQualifier
    @Singleton
    static class SimpleBean {

        public String ping() {
            return "PONG";
        }

    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface NotAQualifier {

    }

}
