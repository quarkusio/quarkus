package io.quarkus.rest.client.reactive.intercepted;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InterceptedRestClientTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestEndpoint.class, Client.class, MyAnnotation.class, MyInterceptorBinding.class,
                            MyInterceptor.class)
                    .addAsResource(new StringAsset(setUrlForClass(Client.class)), "application.properties"));

    @Inject
    @RestClient
    Client client;

    @Test
    public void testFallbackWasUsed() {
        assertEquals("skipped", client.hello("test"));
        assertEquals("pong", client.ping("test"));
    }

    @Path("/test")
    public static class TestEndpoint {
        @GET
        public String get() {
            return "pong";
        }
    }

    @RegisterRestClient
    @MyInterceptorBinding
    public interface Client {
        @GET
        @Path("/{path}")
        String hello(@MyAnnotation("skip") @PathParam("path") String path);

        @GET
        @Path("/{path}")
        String ping(@MyAnnotation("don't skip") @PathParam("path") String param);
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MyAnnotation {
        String value();
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    public static class MyInterceptor {
        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            for (Parameter parameter : ctx.getMethod().getParameters()) {
                MyAnnotation anno = parameter.getAnnotation(MyAnnotation.class);
                if (anno != null && "skip".equals(anno.value())) {
                    return "skipped";
                }
            }
            return ctx.proceed();
        }
    }
}
