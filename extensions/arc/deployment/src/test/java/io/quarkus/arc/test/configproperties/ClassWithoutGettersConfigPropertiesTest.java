package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassWithoutGettersConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.my-enum=OPTIONAL\ndummy.other-enum=Enum_Two\ndummy.numbers=1,2,3,4\ndummy.unused=whatever"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        DummyProperties dummyProperties = dummyBean.dummyProperties;
        assertEquals("quarkus", dummyProperties.name);
        assertEquals(Arrays.asList(1, 2, 3, 4), dummyProperties.numbers);
        assertEquals(MyEnum.OPTIONAL, dummyProperties.myEnum);
        assertEquals(MyEnum.ENUM_ONE, dummyProperties.unsetEnum);
        assertEquals(MyEnum.Enum_Two, dummyProperties.otherEnum);
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;
    }

    @ConfigProperties(prefix = "dummy")
    public static class DummyProperties {

        public String name;
        public List<Integer> numbers;
        public MyEnum myEnum;
        public MyEnum otherEnum = MyEnum.ENUM_ONE;
        public MyEnum unsetEnum = MyEnum.ENUM_ONE;

        public void setName(String name) {
            this.name = name;
        }

        public void setNumbers(List<Integer> numbers) {
            this.numbers = numbers;
        }
    }

    public enum MyEnum {
        OPTIONAL,
        ENUM_ONE,
        Enum_Two
    }
}
