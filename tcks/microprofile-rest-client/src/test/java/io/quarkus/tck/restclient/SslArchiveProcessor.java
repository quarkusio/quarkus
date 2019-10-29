package io.quarkus.tck.restclient;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class SslArchiveProcessor implements ApplicationArchiveProcessor {
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
    }
}
