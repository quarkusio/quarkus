package io.quarkus.tck.opentelemetry;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.testng.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import io.quarkus.runtime.StartupEvent;

public class TestApplication extends Arquillian {
    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @Test
    public void rest() {
        String uri = url.toExternalForm() + "/rest";
        WebTarget echoEndpointTarget = ClientBuilder.newClient().target(uri);
        Response response = echoEndpointTarget.request(TEXT_PLAIN).get();
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
    }

    @Path("/rest")
    public static class TestEndpoint {
        @Inject
        HelloBean helloBean;
        @Inject
        LogHandlerService logHandlerService;

        @GET
        public String hello() {
            return helloBean.hello();
        }
    }

    @ApplicationScoped
    public static class HelloBean {
        public String hello() {
            return "hello";
        }
    }

    @ApplicationScoped
    public static class LogHandlerService {

        @Inject
        @ConfigProperty(name = "quarkus.log.file.path", defaultValue = "target/out.log")
        String pathPattern;

        void onStart(@Observes StartupEvent ev) {
            try {
                Logger rootLogger = LogManager.getLogManager().getLogger("");
                Handler handler = new RedirectOutHandler(pathPattern);
                handler.setLevel(Level.FINE);
                LogManager.getLogManager().getLogger("").addHandler(handler);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
