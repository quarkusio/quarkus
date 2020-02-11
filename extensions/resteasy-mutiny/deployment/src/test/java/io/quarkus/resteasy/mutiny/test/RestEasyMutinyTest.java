package io.quarkus.resteasy.mutiny.test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.mutiny.test.annotations.Async;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestEasyMutinyTest {

    private static AtomicReference<Object> value = new AtomicReference<>();

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MutinyResource.class, MutinyInjector.class, Async.class));

    @TestHTTPResource
    URL url;

    private ResteasyClient client;

    @BeforeEach
    public void before() {
        client = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                .readTimeout(5, TimeUnit.SECONDS)
                .connectionCheckoutTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();
        value.set(null);
        CountDownLatch latch = new CountDownLatch(1);
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void testInjection() {
        Integer data = client.target(url.toExternalForm() + "/injection").request().get(Integer.class);
        Assertions.assertEquals((Integer) 42, data);

        data = client.target(url.toExternalForm() + "/injection-async").request().get(Integer.class);
        Assertions.assertEquals((Integer) 42, data);
    }

}
