package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

public class SubResourceFieldInjectionTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);
                    war.addClasses(StoreResource.class);
                    war.addClasses(OrderResource.class);
                    war.addClasses(ContactResource.class);
                    war.addClasses(ContactResourceImpl.class);
                    war.addClasses(PositionResourceImpl.class);
                    return war;
                }
            }).debugBytecode(true);

    @Test
    public void basicTest() {
        // Test parameter injection works for Class<SubResource> locators
        {
            Client client = ClientBuilder.newClient();
            Response response = client.target(
                    PortProviderUtil.generateURL(
                            "/store/orders/orderId/contacts?typeFilter=SENDER",
                            SubResourceFieldInjectionTest.class.getSimpleName()))
                    .request().get();
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals("[name1]", response.readEntity(String.class), "Wrong content of response");
            response.close();
            client.close();
        }
        {
            Client client = ClientBuilder.newClient();
            Response response = client.target(
                    PortProviderUtil.generateURL(
                            "/store/orders/orderId/contacts",
                            SubResourceFieldInjectionTest.class.getSimpleName()))
                    .request().get();
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals("[name1, name2]", response.readEntity(String.class), "Wrong content of response");
            response.close();
            client.close();
        }

        // Test parameter injection works for @Inject subresources
        {
            Client client = ClientBuilder.newClient();
            Response response = client.target(
                    PortProviderUtil.generateURL(
                            "/store/orders/orderId/positions/positionId",
                            SubResourceFieldInjectionTest.class.getSimpleName()))
                    .request().get();
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals("orderId:positionId", response.readEntity(String.class), "Wrong content of response");
            response.close();
            client.close();
        }
    }

    @Path("store")
    public static class StoreResource {
        @Inject
        PositionResourceImpl positionResource;

        @Path("orders/{order-id}")
        public OrderResource get(@RestPath String id) {

            return new OrderResource() {

                @Override
                public PositionResourceImpl get(String id) {
                    return positionResource;
                }

                @Override
                public Class<ContactResource> contacts() {
                    return (Class<ContactResource>) (Object) ContactResourceImpl.class;
                }
            };
        }
    }

    public interface OrderResource {
        @Path("positions/{positionId}")
        PositionResourceImpl get(@RestPath String id);

        @Path("contacts")
        Class<ContactResource> contacts();
    }

    public interface ContactResource {
        @GET
        List<String> getContactNames(@RestQuery @DefaultValue("123") String typeFilter);
    }

    public static class ContactResourceImpl implements ContactResource {

        @RestQuery
        @DefaultValue("123")
        public String typeFilter;

        @Override
        public List<String> getContactNames(@RestQuery @DefaultValue("123") String typeFilter) {
            if (Objects.equals(typeFilter, "SENDER")) {
                return List.of("name1");
            }
            return List.of("name1", "name2");
        }
    }

    @RequestScoped
    public static class PositionResourceImpl {
        @RestPath("order-id")
        public String orderId;

        @RestPath
        public String positionId;

        @GET
        public String get() {
            return orderId + ":" + positionId;
        }
    }
}
