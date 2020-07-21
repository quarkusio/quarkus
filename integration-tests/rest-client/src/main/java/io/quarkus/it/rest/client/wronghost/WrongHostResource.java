package io.quarkus.it.rest.client.wronghost;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

@Path("/wrong-host")
public class WrongHostResource {

    @GET
    @Path("/rest-client")
    @Produces(MediaType.TEXT_PLAIN)
    public String restClient() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");

        // the system props are set in pom.xml and made available for native tests via RestClientTestResource
        ks.load(new FileInputStream(System.getProperty("rest-client.trustStore")),
                System.getProperty("rest-client.trustStorePassword").toCharArray());

        return RestClientBuilder.newBuilder().baseUrl(new URL("https://wrong.host.badssl.com/")).trustStore(ks)
                .hostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build(WrongHostClient.class)
                .root();
    }
}
