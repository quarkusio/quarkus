package org.acme;

import io.fabric8.kubernetes.client.KubernetesClient;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
