package org.acme;

import io.fabric8.kubernetes.client.KubernetesClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

    @Inject
    BeanWithInjection beanWithInjection;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        KubernetesClient kubernetesClient = beanWithInjection.getKubernetesClient();
        kubernetesClient.pods(); // fails with NullPointerException
        return "hello";
    }
}
