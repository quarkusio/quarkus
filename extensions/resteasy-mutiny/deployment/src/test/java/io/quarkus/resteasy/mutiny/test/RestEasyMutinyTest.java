package io.quarkus.resteasy.mutiny.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.mutiny.runtime.MultiRxInvoker;
import io.quarkus.resteasy.mutiny.runtime.UniRxInvoker;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class RestEasyMutinyTest {

    private CountDownLatch latch;
    private static AtomicReference<Object> value = new AtomicReference<>();
    private static final Logger LOG = Logger.getLogger(RestEasyMutinyTest.class);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MutinyResource.class, MutinyInjector.class));

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
        latch = new CountDownLatch(1);
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void testUni() throws InterruptedException {
        Uni<Response> uni = client.target(url.toExternalForm() + "/uni").request().rx(UniRxInvoker.class).get();
        uni.subscribe().with(
                response -> {
                    value.set(response.readEntity(String.class));
                    latch.countDown();
                }, failure -> LOG.error(failure.getMessage(), failure));
        latch.await();
        Assertions.assertEquals("hello", value.get());
    }

    @Test
    public void testMulti() throws InterruptedException {
        MultiRxInvoker invoker = client.target(url.toExternalForm() + "/multi").request().rx(MultiRxInvoker.class);
        @SuppressWarnings("unchecked")
        Multi<String> multi = (Multi<String>) invoker.get();
        List<String> data = new ArrayList<>();
        multi.subscribe().with(
                data::add,
                failure -> LOG.error(failure.getMessage(), failure),
                () -> latch.countDown());
        latch.await();
        Assertions.assertArrayEquals(new String[] { "hello", "world" }, data.toArray());
    }

    @Test
    public void testInjection() {
        Integer data = client.target(url.toExternalForm() + "/injection").request().get(Integer.class);
        Assertions.assertEquals((Integer) 42, data);

        data = client.target(url.toExternalForm() + "/injection-async").request().get(Integer.class);
        Assertions.assertEquals((Integer) 42, data);
    }

}
