package io.quarkus.arc.test.clientproxy.packageprivate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.clientproxy.packageprivate.foo.MyInterface2;
import io.quarkus.arc.test.clientproxy.packageprivate.foo.Producer;

public class PackagePrivateInterfaceInHierarchyTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BaseInterface.class, MyInterface.class, MyInterface2.class,
            Producer.class);

    @Test
    public void testProducer() {
        assertEquals("quarkus", Arc.container().instance(MyInterface2.class).get().ping());
    }

}
