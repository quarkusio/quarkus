package io.quarkus.smallrye.metrics.stereotype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.metrics.stereotype.stereotypes.CountMe;
import io.quarkus.test.QuarkusUnitTest;

public class StereotypeCountedClassTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CountedClass.class, CountMe.class));

    @Inject
    MetricRegistry metricRegistry;

    @Inject
    CountedClass bean;

    @Test
    public void test() {
        MetricID id_constructor = new MetricID(CountedClass.class.getName() + ".CountedClass");
        assertTrue(metricRegistry.getCounters().containsKey(id_constructor));
        MetricID id_method = new MetricID(CountedClass.class.getName() + ".foo");
        assertTrue(metricRegistry.getCounters().containsKey(id_method));
        bean.foo();
        assertEquals(1, metricRegistry.getCounters().get(id_method).getCount());
    }

}
