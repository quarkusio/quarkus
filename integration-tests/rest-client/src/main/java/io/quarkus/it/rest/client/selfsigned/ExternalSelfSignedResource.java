package io.quarkus.it.rest.client.selfsigned;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * This has nothing to do with rest-client, but we add it here in order to avoid creating
 * a new integration test that would slow down our CI
 */
@Path("/self-signed")
public class ExternalSelfSignedResource {

    @Inject
    @RestClient
    ExternalSelfSignedClient client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String perform() throws IOException {
        return String.valueOf(client.invoke().getStatus());
    }

    @GET
    @Path("/java")
    @Produces(MediaType.TEXT_PLAIN)
    public String invokeJavaURLWithDefaultTruststore() throws IOException {
        try {
            return doGetCipher();
        } catch (IOException e) {
            // if it fails it might be because the remote service is down, so sleep and try again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            return doGetCipher();
        }
    }

    private String doGetCipher() throws IOException {
        // this URL provides an always on example of an HTTPS URL utilizing self-signed certificate
        URL url = new URL("https://self-signed.badssl.com/");
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.getResponseCode();
        return con.getCipherSuite();
    }

}
