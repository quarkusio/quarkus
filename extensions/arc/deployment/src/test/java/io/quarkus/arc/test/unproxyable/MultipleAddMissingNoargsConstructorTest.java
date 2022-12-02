package io.quarkus.arc.test.unproxyable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleAddMissingNoargsConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, MyBeanProducer.class));

    @Inject
    Instance<MyBean> myBeanInstances;

    @Test
    public void testBeansProperlyCreated() {
        List<String> vals = new ArrayList<>(2);
        for (MyBean myBeanInstance : myBeanInstances) {
            vals.add(myBeanInstance.getVal());
        }
        Collections.sort(vals);
        Assertions.assertEquals(Arrays.asList("val1", "val2"), vals);
    }

    static class MyBean {

        private final String val;

        MyBean(String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }
    }

    @Dependent
    static class MyBeanProducer {

        @ApplicationScoped
        public MyBean myBean1() {
            return new MyBean("val1");
        }

        @ApplicationScoped
        public MyBean myBean2() {
            return new MyBean("val2");
        }
    }

}
