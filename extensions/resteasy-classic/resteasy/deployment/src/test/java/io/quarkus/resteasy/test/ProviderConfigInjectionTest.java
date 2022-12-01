package io.quarkus.resteasy.test;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ProviderConfigInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClasses(TestResource.class, FooProvider.class)
                    .addAsResource(new StringAsset("foo=bar"), "application.properties"));

    @Test
    public void testPropertyInjection() {
        RestAssured.when().get("/test").then().body(Matchers.is("bar"));
    }

    @Path("/test")
    public static class TestResource {

        @Context
        private Providers providers;

        @GET
        public String getFoo() {
            return providers.getContextResolver(String.class, MediaType.TEXT_PLAIN_TYPE).getContext(null);
        }
    }

    @Provider
    public static class FooProvider implements ContextResolver<String> {

        private final BeanManager beanManager;

        @ConfigProperty(name = "foo")
        Instance<String> foo;

        @Inject
        public FooProvider(BeanManager beanManager) {
            this.beanManager = beanManager;
        }

        @Override
        public String getContext(Class<?> type) {
            Assertions.assertNotNull(beanManager);
            return foo.get();
        }
    }
}
