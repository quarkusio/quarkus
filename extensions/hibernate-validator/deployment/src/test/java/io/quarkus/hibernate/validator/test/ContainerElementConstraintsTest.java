package io.quarkus.hibernate.validator.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ContainerElementConstraintsTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class).addClasses(TestBean.class));

    @Test
    public void testContainerElementConstraint() {
        assertThat(validatorFactory.getValidator().validate(new TestBean())).hasSize(1);
    }

    @Test
    public void testNestedContainerElementConstraint() {
        assertThat(validatorFactory.getValidator().validate(new NestedTestBean())).hasSize(1);
    }

    @Test
    public void testMethodParameterContainerElementConstraint() throws NoSuchMethodException, SecurityException {
        Map<String, List<String>> invalidMap = new HashMap<>();
        invalidMap.put("key", Collections.singletonList(""));

        assertThat(validatorFactory.getValidator().forExecutables().validateParameters(new MethodParameterTestBean(),
                MethodParameterTestBean.class.getMethod("test", Map.class), new Object[] { invalidMap })).hasSize(1);
    }

    @Test
    public void testMethodReturnValueContainerElementConstraint() throws NoSuchMethodException, SecurityException {
        Map<String, List<String>> invalidMap = new HashMap<>();
        invalidMap.put("key", Collections.singletonList(""));

        assertThat(validatorFactory.getValidator().forExecutables().validateReturnValue(new MethodReturnValueTestBean(),
                MethodReturnValueTestBean.class.getMethod("test"), invalidMap)).hasSize(1);
    }

    @Test
    public void testConstructorParameterContainerElementConstraint() throws NoSuchMethodException, SecurityException {
        List<String> invalidList = Collections.singletonList("");

        assertThat(validatorFactory.getValidator().forExecutables().validateConstructorParameters(
                ConstructorParameterTestBean.class.getConstructor(List.class), new Object[] { invalidList })).hasSize(1);
    }

    static class TestBean {

        public Map<String, @NotBlank String> constrainedMap;

        public TestBean() {
            Map<String, String> invalidMap = new HashMap<>();
            invalidMap.put("key", "");

            this.constrainedMap = invalidMap;
        }
    }

    static class NestedTestBean {

        public Map<String, List<@NotBlank String>> constrainedMap;

        public NestedTestBean() {
            Map<String, List<String>> invalidMap = new HashMap<>();
            invalidMap.put("key", Collections.singletonList(""));

            this.constrainedMap = invalidMap;
        }
    }

    static class MethodParameterTestBean {

        public void test(Map<String, List<@NotBlank String>> constrainedMap) {
            // do nothing
        }
    }

    static class MethodReturnValueTestBean {

        public Map<String, List<@NotBlank String>> test() {
            return null;
        }
    }

    static class ConstructorParameterTestBean {

        public ConstructorParameterTestBean(List<@NotBlank String> constrainedList) {
            // do nothing
        }
    }
}
