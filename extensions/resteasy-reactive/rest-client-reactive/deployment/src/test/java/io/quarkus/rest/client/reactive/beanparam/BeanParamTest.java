package io.quarkus.rest.client.reactive.beanparam;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class BeanParamTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldPassPathParamFromBeanParam() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.beanParamWithFields(new MyBeanParamWithFields()))
                .isEqualTo("restPathDefault/restPathOverridden/pathParam"
                        + "/restHeaderDefault/restHeaderOverridden/headerParam"
                        + "/restFormDefault/restFormOverridden/formParam"
                        + "/restCookieDefault/restCookieOverridden/cookieParam"
                        + "/restQueryDefault/restQueryOverridden/queryParam");
        assertThat(client.regularParameters(
                "restPathDefault", "restPathOverridden", "pathParam",
                "restHeaderDefault", "restHeaderOverridden", "headerParam",
                "restCookieDefault", "restCookieOverridden", "cookieParam",
                "restFormDefault", "restFormOverridden", "formParam",
                "restQueryDefault", "restQueryOverridden", "queryParam"))
                .isEqualTo("restPathDefault/restPathOverridden/pathParam"
                        + "/restHeaderDefault/restHeaderOverridden/headerParam"
                        + "/restFormDefault/restFormOverridden/formParam"
                        + "/restCookieDefault/restCookieOverridden/cookieParam"
                        + "/restQueryDefault/restQueryOverridden/queryParam");
        assertThat(client.beanParamWithProperties(new MyBeanParamWithProperties())).isEqualTo("null/null/pathParam"
                + "/null/null/headerParam"
                + "/null/null/formParam"
                + "/null/null/cookieParam"
                + "/null/null/queryParam");
    }

    public interface Client {
        @Path("/{restPathDefault}/{restPath_Overridden}/{pathParam}")
        @POST
        String beanParamWithFields(MyBeanParamWithFields beanParam);

        @Path("/{restPathDefault}/{restPath_Overridden}/{pathParam}")
        @POST
        String regularParameters(@RestPath String restPathDefault,
                @RestPath("restPath_Overridden") String restPathOverridden,
                @PathParam("pathParam") String pathParam,

                @RestHeader String restHeaderDefault,
                @RestHeader("restHeader_Overridden") String restHeaderOverridden,
                @HeaderParam("headerParam") String headerParam,

                @RestCookie String restCookieDefault,
                @RestCookie("restCookie_Overridden") String restCookieOverridden,
                @CookieParam("cookieParam") String cookieParam,

                @RestForm String restFormDefault,
                @RestForm("restForm_Overridden") String restFormOverridden,
                @FormParam("formParam") String formParam,

                @RestQuery String restQueryDefault,
                @RestQuery("restQuery_Overridden") String restQueryOverridden,
                @QueryParam("queryParam") String queryParam);

        @Path("/{pathParam}")
        @POST
        String beanParamWithProperties(MyBeanParamWithProperties beanParam);
    }

    public static class MyBeanParamWithFields {
        @RestPath
        private String restPathDefault = "restPathDefault";
        @RestPath("restPath_Overridden")
        private String restPathOverridden = "restPathOverridden";
        @PathParam("pathParam")
        private String pathParam = "pathParam";

        @RestHeader
        private String restHeaderDefault = "restHeaderDefault";
        @RestHeader("restHeader_Overridden")
        private String restHeaderOverridden = "restHeaderOverridden";
        @HeaderParam("headerParam")
        private String headerParam = "headerParam";

        @RestCookie
        private String restCookieDefault = "restCookieDefault";
        @RestCookie("restCookie_Overridden")
        private String restCookieOverridden = "restCookieOverridden";
        @CookieParam("cookieParam")
        private String cookieParam = "cookieParam";

        @RestForm
        private String restFormDefault = "restFormDefault";
        @RestForm("restForm_Overridden")
        private String restFormOverridden = "restFormOverridden";
        @FormParam("formParam")
        private String formParam = "formParam";

        @RestQuery
        private String restQueryDefault = "restQueryDefault";
        @RestQuery("restQuery_Overridden")
        private String restQueryOverridden = "restQueryOverridden";
        @QueryParam("queryParam")
        private String queryParam = "queryParam";

        // FIXME: Matrix not supported
    }

    public static class MyBeanParamWithProperties {
        // FIXME: not allowed yet
        //        @RestPath
        //        public String getRestPathDefault(){
        //            return "restPathDefault";
        //        }
        //        @RestPath("restPath_Overridden")
        //        public String getRestPathOverridden(){
        //            return "restPathOverridden";
        //        }
        @PathParam("pathParam")
        public String getPathParam() {
            return "pathParam";
        }

        //        @RestHeader
        //        public String getRestHeaderDefault(){
        //            return "restHeaderDefault";
        //        }
        //        @RestHeader("restHeader_Overridden")
        //        public String getRestHeaderOverridden(){
        //            return "restHeaderOverridden";
        //        }
        @HeaderParam("headerParam")
        public String getHeaderParam() {
            return "headerParam";
        }

        //        @RestCookie
        //        public String getRestCookieDefault(){
        //            return "restCookieDefault";
        //        }
        //        @RestCookie("restCookie_Overridden")
        //        public String getRestCookieOverridden(){
        //            return "restCookieOverridden";
        //        }
        @CookieParam("cookieParam")
        public String getCookieParam() {
            return "cookieParam";
        }

        //        @RestForm
        //        public String getRestFormDefault(){
        //            return "restFormDefault";
        //        }
        //        @RestForm("restForm_Overridden")
        //        public String getRestFormOverridden(){
        //            return "restFormOverridden";
        //        }
        @FormParam("formParam")
        public String getFormParam() {
            return "formParam";
        }

        //        @RestQuery
        //        public String getRestQueryDefault(){
        //            return "restQueryDefault";
        //        }
        //        @RestQuery("restQuery_Overridden")
        //        public String getRestQueryOverridden(){
        //            return "restQueryOverridden";
        //        }
        @QueryParam("queryParam")
        public String getQueryParam() {
            return "queryParam";
        }

        // FIXME: Matrix not supported
    }

    @Path("/")
    public static class Resource {
        @Path("/{restPathDefault}/{restPath_Overridden}/{pathParam}")
        @POST
        public String beanParamWithFields(@RestPath String restPathDefault,
                @RestPath String restPath_Overridden,
                @RestPath String pathParam,
                @RestHeader String restHeaderDefault,
                @RestHeader("restHeader_Overridden") String restHeader_Overridden,
                @RestHeader("headerParam") String headerParam,
                @RestForm String restFormDefault,
                @RestForm String restForm_Overridden,
                @RestForm String formParam,
                @RestCookie String restCookieDefault,
                @RestCookie String restCookie_Overridden,
                @RestCookie String cookieParam,
                @RestQuery String restQueryDefault,
                @RestQuery String restQuery_Overridden,
                @RestQuery String queryParam) {
            return restPathDefault + "/" + restPath_Overridden + "/" + pathParam
                    + "/" + restHeaderDefault + "/" + restHeader_Overridden + "/" + headerParam
                    + "/" + restFormDefault + "/" + restForm_Overridden + "/" + formParam
                    + "/" + restCookieDefault + "/" + restCookie_Overridden + "/" + cookieParam
                    + "/" + restQueryDefault + "/" + restQuery_Overridden + "/" + queryParam;
        }

        @Path("/{pathParam}")
        @POST
        public String beanParamWithProperties(@RestPath String restPathDefault,
                @RestPath String restPath_Overridden,
                @RestPath String pathParam,
                @RestHeader String restHeaderDefault,
                @RestHeader("restHeader_Overridden") String restHeader_Overridden,
                @RestHeader("headerParam") String headerParam,
                @RestForm String restFormDefault,
                @RestForm String restForm_Overridden,
                @RestForm String formParam,
                @RestCookie String restCookieDefault,
                @RestCookie String restCookie_Overridden,
                @RestCookie String cookieParam,
                @RestQuery String restQueryDefault,
                @RestQuery String restQuery_Overridden,
                @RestQuery String queryParam) {
            return restPathDefault + "/" + restPath_Overridden + "/" + pathParam
                    + "/" + restHeaderDefault + "/" + restHeader_Overridden + "/" + headerParam
                    + "/" + restFormDefault + "/" + restForm_Overridden + "/" + formParam
                    + "/" + restCookieDefault + "/" + restCookie_Overridden + "/" + cookieParam
                    + "/" + restQueryDefault + "/" + restQuery_Overridden + "/" + queryParam;
        }
    }
}
