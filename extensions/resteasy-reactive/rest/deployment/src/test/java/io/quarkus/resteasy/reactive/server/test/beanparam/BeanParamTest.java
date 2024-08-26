package io.quarkus.resteasy.reactive.server.test.beanparam;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BeanParamTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> {
                return ShrinkWrap.create(JavaArchive.class)
                        .addClasses(MyBeanParamWithFieldsAndProperties.class, Top.class);
            });

    @Test
    void shouldDeployWithoutIssues() {
        // Apparently, Top and MyBeanParamWithFieldsAndProperties are only there to check that it deploys
        // probably we test that they run in another test?
    }

    public static class Top {
        @PathParam("pathParam")
        private String pathParam = "pathParam";

        public String getPathParam() {
            return pathParam;
        }

        public void setPathParam(String pathParam) {
            this.pathParam = pathParam;
        }
    }

    public static class MyBeanParamWithFieldsAndProperties extends Top {
        @HeaderParam("headerParam")
        private String headerParam = "headerParam";
        @CookieParam("cookieParam")
        private String cookieParam = "cookieParam";
        @FormParam("formParam")
        private String formParam = "formParam";
        @QueryParam("queryParam")
        private String queryParam = "queryParam";

        // FIXME: Matrix not supported

        public String getHeaderParam() {
            return headerParam;
        }

        public void setHeaderParam(String headerParam) {
            this.headerParam = headerParam;
        }

        public String getCookieParam() {
            return cookieParam;
        }

        public void setCookieParam(String cookieParam) {
            this.cookieParam = cookieParam;
        }

        public String getFormParam() {
            return formParam;
        }

        public void setFormParam(String formParam) {
            this.formParam = formParam;
        }

        public String getQueryParam() {
            return queryParam;
        }

        public void setQueryParam(String queryParam) {
            this.queryParam = queryParam;
        }
    }

    @Path("/")
    public static class Resource {
        @Path("/a/{restPathDefault}/{restPath_Overridden}/{pathParam}")
        @POST
        public String beanParamWithFields(@BeanParam MyBeanParamWithFieldsAndProperties p) {
            return null;
        }

        @Path("/b/{pathParam}")
        @POST
        public String beanParamWithFields(@BeanParam Top p) {
            return null;
        }
    }
}
