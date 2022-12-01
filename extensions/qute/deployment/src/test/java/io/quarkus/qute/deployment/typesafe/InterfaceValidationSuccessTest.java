package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class InterfaceValidationSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Metrics.class, Count.class, Wrapper.class, NumericWrapper.class)
                    .addAsResource(new StringAsset("{@java.util.List list}"
                            + "{list.empty}:{list.toString}"),
                            "templates/list.html")
                    .addAsResource(
                            new StringAsset(
                                    "{@io.quarkus.qute.deployment.typesafe.InterfaceValidationSuccessTest$Metrics metrics}"
                                            + "{metrics.responses.value}:{metrics.responses.name(1)}:{metrics.requests.value??}"),
                            "templates/metrics.html"));

    @Inject
    Template list;

    @Inject
    Template metrics;

    @Test
    public void testInterfaceMethod() {
        assertEquals("true:[]", list.data("list", Collections.emptyList()).render());
    }

    @Test
    public void testInterfaceHierarchy() {
        assertEquals("5:Andy:",
                metrics.data("metrics", new Metrics() {

                    @Override
                    public Count responses() {
                        return new Count() {

                            @Override
                            public Integer value() {
                                return 5;
                            }

                            @Override
                            public String name(int age) {
                                return "Andy";
                            }
                        };
                    }

                    @Override
                    public Count requests() {
                        return null;
                    }
                }).render());

    }

    public interface Wrapper<T> {
        T value();

        String name(int age);
    }

    public interface NumericWrapper extends Wrapper<Integer> {
    }

    public interface Count extends NumericWrapper {
    }

    public interface Metrics {

        Count requests();

        Count responses();
    }
}
