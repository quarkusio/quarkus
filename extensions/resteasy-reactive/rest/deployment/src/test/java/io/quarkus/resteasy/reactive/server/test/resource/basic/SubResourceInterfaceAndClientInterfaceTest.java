package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.server.deployment.SetupEndpointsResultBuildItem;
import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

public class SubResourceInterfaceAndClientInterfaceTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);
                    war.addClasses(StoreResource.class);
                    war.addClasses(OrderResource.class);
                    war.addClasses(PositionResource.class);
                    war.addClasses(PositionResourceImpl.class);
                    war.addClasses(UndangerousGoodsResource.class);
                    war.addClasses(DangerousGoodsResource.class);
                    war.addClasses(VeryDangerousGoodsResource.class);
                    war.addClasses(SubResourceRestClientInterface.class);
                    war.addClasses(ContactResource.class);
                    war.addClasses(ContactResourceImpl.class);
                    return war;
                }
            })
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            SetupEndpointsResultBuildItem consumed = context.consume(SetupEndpointsResultBuildItem.class);
                            context.produce(new FeatureBuildItem("just-here-to-invoke-buildstep"));

                            for (ResourceClass subResourceClass : consumed.getSubResourceClasses()) {
                                if (subResourceClass.getClassName().contains("SubResourceRestClientInterface")) {
                                    throw new IllegalStateException(
                                            "Client Interface SubResourceRestClientInterface got endpoint indexed.");
                                }
                            }
                        }
                    }).consumes(SetupEndpointsResultBuildItem.class).produces(FeatureBuildItem.class).build();
                }
            });

    @Test
    public void basicTest() {
        {
            Client client = ClientBuilder.newClient();
            Response response = client.target(
                    PortProviderUtil.generateURL(
                            "/store/orders/orderId/positions/positionId/dangerousgoods/dangerousgoodId/some-field",
                            SubResourceInterfaceAndClientInterfaceTest.class.getSimpleName()))
                    .request().get();
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals("someFielddangerousgoodId", response.readEntity(String.class), "Wrong content of response");
            response.close();
            client.close();
        }

        {
            Client client = ClientBuilder.newClient();
            Response response = client.target(
                    PortProviderUtil.generateURL(
                            "/store/orders/orderId/contacts",
                            SubResourceInterfaceAndClientInterfaceTest.class.getSimpleName()))
                    .request().get();
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals("[name1, name2]", response.readEntity(String.class), "Wrong content of response");
            response.close();
            client.close();
        }
    }

    @Path("store")
    public static class StoreResource {
        @Path("orders/{id}")
        public OrderResource get(@RestPath String id) {

            return new OrderResource() {

                @Override
                public PositionResource get(String id) {
                    return new PositionResourceImpl(id);
                }

                @Override
                public Class<ContactResource> contacts() {
                    return (Class<ContactResource>) (Object) ContactResourceImpl.class;
                }
            };
        }

        @Path("user-count")
        public Long getUserCount() {
            return 4L;
        }
    }

    public interface OrderResource {
        @Path("positions/{id}")
        PositionResource get(@RestPath String id);

        @Path("contacts")
        Class<ContactResource> contacts();
    }

    public interface ContactResource {
        @GET
        List<String> getContactNames();
    }

    public static class ContactResourceImpl implements ContactResource {

        @Override
        public List<String> getContactNames() {
            return List.of("name1", "name2");
        }
    }

    public interface PositionResource {
        @Path("dangerousgoods/{id}")
        UndangerousGoodsResource get(@RestPath String id);
    }

    public static class PositionResourceImpl implements PositionResource {

        private final String id;

        public PositionResourceImpl(String id) {
            this.id = id;
        }

        @Override
        public UndangerousGoodsResource get(String id) {
            InvocationHandler handler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("getSomeField")) {
                        return "someField" + id;
                    }
                    return null;
                }
            };
            Class[] intfs = { VeryDangerousGoodsResource.class };
            return (VeryDangerousGoodsResource) Proxy.newProxyInstance(PositionResourceImpl.class.getClassLoader(), intfs,
                    handler);
        }
    }

    public interface UndangerousGoodsResource {
        // not even dangerous enough to get a resource method
    }

    public interface DangerousGoodsResource extends UndangerousGoodsResource {
        @GET
        String get();
    }

    public interface VeryDangerousGoodsResource extends DangerousGoodsResource {
        @GET
        @Path("some-field")
        String getSomeField();
    }

    public interface SubResourceRestClientInterface {
        @GET
        String getSomeField();
    }
}
