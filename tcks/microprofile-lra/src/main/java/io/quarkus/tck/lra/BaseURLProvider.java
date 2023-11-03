package io.quarkus.tck.lra;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class BaseURLProvider implements ResourceProvider {
    @Override
    public boolean canProvide(Class<?> aClass) {
        return aClass.isAssignableFrom(URL.class);
    }

    @Override
    public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {
        String testHost = ConfigProvider.getConfig().getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        Integer testPort = ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class).orElse(8081);

        try {
            return URI.create(String.format("http://%s:%d", testHost, testPort)).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
