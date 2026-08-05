package io.quarkus.extest;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.net.CidrAddress;

public class PackageTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(PackageTestCase.class);
        }
    });

    @Test
    public void testVersionInPackage() {
        Assertions.assertNotNull(CidrAddress.class.getPackage().getImplementationVersion());
    }

    @Test
    public void testAssumptionsWork() {
        //these were broken at one point
        Assumptions.assumeTrue(false);
    }

}
