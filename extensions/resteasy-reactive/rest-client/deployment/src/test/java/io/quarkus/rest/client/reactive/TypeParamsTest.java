package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusUnitTest;

public class TypeParamsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Pojo.class, Foo.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.client.url", "http://localhost:${quarkus.http.test-port:8081}");

    @RestClient
    Client client;

    @Test
    void test() {
        assertThat(ClientProxy.unwrap(client).getClass()).hasMethods("call").satisfies(cl -> {
            Method callMethod = cl.getMethod("call", List.class);
            assertThat(callMethod.getAnnotation(Other.class)).isNull();

            AnnotatedType annotatedType = callMethod.getAnnotatedParameterTypes()[0];
            assertThat(annotatedType).isInstanceOfSatisfying(AnnotatedParameterizedType.class, apt -> {
                AnnotatedType[] typeArguments = apt.getAnnotatedActualTypeArguments();
                assertThat(typeArguments).singleElement().satisfies(t -> {
                    assertThat(t.getAnnotation(Foo.class)).isNotNull();
                });
            });
        });

        assertThat(ClientProxy.unwrap(client).getClass()).hasMethods("call2").satisfies(cl -> {
            Method callMethod = cl.getMethod("call2", String.class, Map.class);
            assertThat(callMethod.getAnnotation(Other.class)).isNotNull();

            AnnotatedType annotatedType1 = callMethod.getAnnotatedParameterTypes()[0];
            assertThat(annotatedType1.getAnnotation(Bar.class)).isNotNull();
            assertThat(annotatedType1.getAnnotation(Foo.class)).isNull();

            AnnotatedType annotatedType2 = callMethod.getAnnotatedParameterTypes()[1];
            assertThat(annotatedType2).isInstanceOfSatisfying(AnnotatedParameterizedType.class, apt -> {
                AnnotatedType[] typeArguments = apt.getAnnotatedActualTypeArguments();
                assertThat(typeArguments).hasSize(2).satisfies(t -> {
                    AnnotatedType first = typeArguments[0];
                    assertThat(first.getAnnotation(Foo.class)).isNotNull();
                    assertThat(first.getAnnotation(Bar.class)).isNull();

                    AnnotatedType second = typeArguments[1];
                    assertThat(second.getAnnotation(Bar.class)).isNotNull();
                    assertThat(second.getAnnotation(Foo.class)).isNull();
                });
            });
        });
    }

    @RegisterRestClient(configKey = "client")
    public interface Client {

        @Path("/")
        @POST
        void call(List<@Foo Pojo> list);

        @Other
        @Path("/2")
        @POST
        void call2(@NotBody @Bar String p1, @NotBody Map<@Foo String, @Bar String> p2);
    }

    public record Pojo(String field) {

    }

    @Target({ ElementType.TYPE_USE, ElementType.PARAMETER })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {
    }

    @Target({ ElementType.TYPE_USE })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bar {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Other {

    }
}
