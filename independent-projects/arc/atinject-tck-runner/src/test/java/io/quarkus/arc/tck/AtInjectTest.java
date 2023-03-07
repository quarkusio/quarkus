package io.quarkus.arc.tck;

import static org.junit.Assert.assertTrue;

import java.util.Enumeration;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.quarkus.arc.Arc;

@RunWith(Arquillian.class)
public class AtInjectTest {
    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, Tck.class.getPackage())
                .addClasses(AtInjectTest.class, AtInjectTckExtension.class, Spare.class)
                .addAsServiceProvider(BuildCompatibleExtension.class, AtInjectTckExtension.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void test() {
        Car instance = Arc.container().instance(Car.class).get();

        junit.framework.Test test = Tck.testsFor(instance, /* supportsStatic */ false, /* supportsPrivate */ true);
        junit.framework.TestResult result = new junit.framework.TestResult();
        test.run(result);

        // this is ugly and doesn't report failures properly, but I don't see a better way
        if (!result.wasSuccessful()) {
            int failuresCount = 0;
            Enumeration<junit.framework.TestFailure> failures = result.failures();
            while (failures.hasMoreElements()) {
                System.out.println(failures.nextElement());
                failuresCount++;
            }

            int errorsCount = 0;
            Enumeration<junit.framework.TestFailure> errors = result.errors();
            while (errors.hasMoreElements()) {
                System.out.println(errors.nextElement());
                errorsCount++;
            }
            System.out.println("Total " + failuresCount + " failures and " + errorsCount + " errors");
        }

        assertTrue(result.wasSuccessful());
    }
}
