package io.quarkus.extest;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.common.net.CidrAddress;

import io.quarkus.test.QuarkusUnitTest;

public class PackageTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(PackageTestCase.class);
        }
    });

    @Test
    public void testVersionInPackage() {
        Assertions.assertNotNull(CidrAddress.class.getPackage().getImplementationVersion());
    }

}
