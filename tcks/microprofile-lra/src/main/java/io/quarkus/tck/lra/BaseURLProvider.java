package io.quarkus.tck.lra;

import java.net.URI;
import java.net.URL;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class BaseURLProvider {

    @Produces
    @Dependent
    public URL produceBaseURL() throws Exception {
        String testHost = System.getProperty("quarkus.http.host", "localhost");
        Integer testPort = Integer.getInteger("quarkus.http.test-port", 8081);
        return URI.create(String.format("http://%s:%d", testHost, testPort)).toURL();
    }
}
