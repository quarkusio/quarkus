package io.quarkus.tck.restclient;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class RestClientProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        // Only apply the processor to SSL tests
        if (testClass.getName().contains("SslHostnameVerifierTest") ||
                testClass.getName().contains("SslMutualTest") ||
                testClass.getName().contains("SslTrustStoreTest") ||
                testClass.getName().contains("SslContextTest")) {

            if (!(applicationArchive instanceof WebArchive)) {
                return;
            }

            WebArchive war = applicationArchive.as(WebArchive.class);
            war.addAsResource(new StringAsset("quarkus.ssl.native=true"), "application.properties");
        }

        // Make sure the test class and all of its superclasses are added to the test deployment
        // This ensures that all the classes from the hierarchy are loaded by the RuntimeClassLoader
        if (ClassContainer.class.isInstance(applicationArchive) && testClass.getJavaClass().getSuperclass() != null) {
            ClassContainer<?> classContainer = ClassContainer.class.cast(applicationArchive);
            Class<?> clazz = testClass.getJavaClass().getSuperclass();
            while (clazz != Object.class && clazz != null && clazz != Arquillian.class) {
                classContainer.addClass(clazz);
                clazz = clazz.getSuperclass();
            }

        }
    }
}
