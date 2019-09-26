package io.quarkus.tck.faulttolerance;

import java.io.File;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ClassContainer;

public class FaultToleranceApplicationArchiveProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (!(applicationArchive instanceof ClassContainer)) {
            return;
        }

        ClassContainer<?> classContainer = (ClassContainer<?>) applicationArchive;
        classContainer.addAsResource(new File("src/test/resources/config.properties"));
    }
}
