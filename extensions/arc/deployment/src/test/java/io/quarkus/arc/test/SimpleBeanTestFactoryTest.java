package io.quarkus.arc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SimpleBeanTestFactoryTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("simpleBean.baz=1"), "application.properties"));

    @Inject
    SimpleBean simpleBean;

    @TestFactory
    public List<DynamicTest> testBeanInsideFactory() {
        return List.of(
                DynamicTest.dynamicTest("test 1", () -> {
                    assertNotNull(simpleBean);
                }),
                DynamicTest.dynamicTest("test 2", () -> {
                    assertEquals(1, 1);
                }));
    }
}
