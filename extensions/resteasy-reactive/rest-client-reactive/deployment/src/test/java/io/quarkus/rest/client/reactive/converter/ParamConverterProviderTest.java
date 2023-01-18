package io.quarkus.rest.client.reactive.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ParamConverterProviderTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldConvertPathParam() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);
        assertThat(client.get(Param.FIRST)).isEqualTo("1");
        assertThat(client.sub().get(Param.SECOND)).isEqualTo("2");

        Bean bean = new Bean();
        bean.param = Param.FIRST;
        assertThat(client.get(bean)).isEqualTo("1");
    }

    @Test
    void shouldConvertQueryParams() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);
        assertThat(client.getWithQuery(Param.FIRST)).isEqualTo("1");
        assertThat(client.sub().getWithQuery(Param.SECOND)).isEqualTo("2");

        Bean bean = new Bean();
        bean.param = Param.SECOND;
        bean.queryParam = Param.FIRST;
        assertThat(client.getWithQuery(bean)).isEqualTo("1");
    }

    @Test
    void shouldConvertHeaderParams() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);
        assertThat(client.getWithHeader(Param.FIRST)).isEqualTo("1");
        assertThat(client.sub().getWithHeader(Param.SECOND)).isEqualTo("2");

        Bean bean = new Bean();
        bean.param = Param.SECOND;
        bean.queryParam = Param.SECOND;
        bean.headerParam = Param.FIRST;
        assertThat(client.getWithHeader(bean)).isEqualTo("1");
    }

    @Test
    void shouldConvertCookieParams() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);
        assertThat(client.getWithHeader(Param.FIRST)).isEqualTo("1");
        assertThat(client.sub().getWithCookie(Param.SECOND)).isEqualTo("2");

        Bean bean = new Bean();
        bean.param = Param.SECOND;
        bean.queryParam = Param.SECOND;
        bean.headerParam = Param.SECOND;
        bean.cookieParam = Param.FIRST;
        assertThat(client.getWithCookie(bean)).isEqualTo("1");
    }

    @Path("/echo")
    @RegisterProvider(ParamConverter.class)
    interface Client {
        @Path("/sub")
        SubClient sub();

        @GET
        @Path("/param/{param}")
        String get(@PathParam("param") Param param);

        @GET
        @Path("/param/{param}")
        String get(@BeanParam Bean beanParam);

        @GET
        @Path("/query")
        String getWithQuery(@QueryParam("param") Param param);

        @GET
        @Path("/query")
        String getWithQuery(@BeanParam Bean beanParam);

        @GET
        @Path("/header")
        String getWithHeader(@HeaderParam("param") Param param);

        @GET
        @Path("/header")
        String getWithHeader(@BeanParam Bean beanParam);

        @GET
        @Path("/cookie")
        String getWithCookie(@HeaderParam("cookie-param") Param param);

        @GET
        @Path("/cookie")
        String getWithCookie(@BeanParam Bean beanParam);
    }

    interface SubClient {
        @GET
        @Path("/param/{param}")
        String get(@PathParam("param") Param param);

        @GET
        @Path("/query")
        String getWithQuery(@QueryParam("param") Param param);

        @GET
        @Path("/header")
        String getWithHeader(@HeaderParam("param") Param param);

        @GET
        @Path("cookie")
        String getWithCookie(@CookieParam("cookie-param") Param param);
    }

    public static class Bean {
        @PathParam("param")
        public Param param;
        @QueryParam("param")
        public Param queryParam;
        @HeaderParam("param")
        public Param headerParam;
        @CookieParam("cookie-param")
        public Param cookieParam;
    }

    enum Param {
        FIRST,
        SECOND
    }

    public static class ParamConverter implements ParamConverterProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T> jakarta.ws.rs.ext.ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
                Annotation[] annotations) {
            if (genericType == null) {
                fail("Generic Type cannot be null!");
            }

            if (annotations == null) {
                fail("Annotations cannot be null!");
            }

            if (rawType == Param.class) {
                return (jakarta.ws.rs.ext.ParamConverter<T>) new jakarta.ws.rs.ext.ParamConverter<Param>() {
                    @Override
                    public Param fromString(String value) {
                        return null;
                    }

                    @Override
                    public String toString(Param value) {
                        if (value == null) {
                            return null;
                        }
                        switch (value) {
                            case FIRST:
                                return "1";
                            case SECOND:
                                return "2";
                            default:
                                return "unexpected";
                        }
                    }
                };
            }
            return null;
        }
    }

    @Path("/echo")
    public static class EchoEndpoint {
        @Path("/param/{param}")
        @GET
        public String echoPath(@PathParam("param") String param) {
            return param;
        }

        @Path("/sub/param/{param}")
        @GET
        public String echoSubPath(@PathParam("param") String param) {
            return param;
        }

        @GET
        @Path("/query")
        public String get(@QueryParam("param") String param) {
            return param;
        }

        @Path("/sub/query")
        @GET
        public String getSub(@QueryParam("param") String param) {
            return param;
        }

        @GET
        @Path("/header")
        public String getHeader(@HeaderParam("param") String param) {
            return param;
        }

        @Path("/sub/header")
        @GET
        public String getSubHeader(@HeaderParam("param") String param) {
            return param;
        }

        @GET
        @Path("/cookie")
        public String getCookie(@CookieParam("cookie-param") String param) {
            return param;
        }

        @Path("/sub/cookie")
        @GET
        public String getSubCookie(@CookieParam("cookie-param") String param) {
            return param;
        }
    }
}
