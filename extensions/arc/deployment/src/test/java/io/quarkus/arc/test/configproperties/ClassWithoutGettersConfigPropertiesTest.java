package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassWithoutGettersConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.numbers=1,2,3,4\ndummy.unused=whatever"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.getName());
        assertEquals(Arrays.asList(1, 2, 3, 4), dummyBean.getNumbers());
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;

        String getName() {
            return dummyProperties.name;
        }

        List<Integer> getNumbers() {
            return dummyProperties.numbers;
        }
    }

    @ConfigProperties(prefix = "dummy")
    public static class DummyProperties {

        public String name;
        public List<Integer> numbers;

        public void setName(String name) {
            this.name = name;
        }

        public void setNumbers(List<Integer> numbers) {
            this.numbers = numbers;
        }
    }
}
