package io.quarkus.resteasy.reactive.server.test.beanparam;

import static org.hamcrest.CoreMatchers.equalTo;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class BeanParamRecordTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> {
                return ShrinkWrap.create(JavaArchive.class)
                        .addClasses(BeanParamRecord.class, OtherBeanParam.class, OtherBeanParamClass.class,
                                OtherBeanParamRecord.class);
            });

    @Test
    void shouldWork() {
        RestAssured
                .given()
                .header("Header-Param", "got it")
                .queryParam("primitiveByte", "2")
                .queryParam("primitiveShort", "3")
                .queryParam("primitiveInt", "4")
                .queryParam("primitiveLong", "5")
                .queryParam("primitiveFloat", "6")
                .queryParam("primitiveDouble", "7")
                .queryParam("primitiveChar", "a")
                .queryParam("primitiveBoolean", "true")
                .queryParam("q", "query")
                .get("/record")
                .then()
                .statusCode(200)
                .body(equalTo("got it/2/3/4/5/6.0/7.0/true/a/query/query/query/query"));

    }

    public record BeanParamRecord(@RestHeader String headerParam,
            @RestQuery byte primitiveByte,
            @RestQuery short primitiveShort,
            @RestQuery int primitiveInt,
            @RestQuery long primitiveLong,
            @RestQuery float primitiveFloat,
            @RestQuery double primitiveDouble,
            @RestQuery boolean primitiveBoolean,
            @RestQuery char primitiveChar,
            UriInfo uriInfo,
            // record contains bp (implicit @BeanParam)
            OtherBeanParam obp,
            // record contains record (implicit @BeanParam)
            OtherBeanParamRecord obpr) {
    }

    public static class OtherBeanParam {
        @RestQuery
        String q;
        // bp contains record
        @BeanParam // no implicit annotation on fields yet
        OtherBeanParamRecord obpr;
        // bp contains bp
        @BeanParam // no implicit annotation on fields yet
        OtherBeanParamClass obpc;
    }

    public record OtherBeanParamRecord(@RestQuery String q) {
    }

    public static class OtherBeanParamClass {
        @RestQuery
        String q;
    }

    @Path("/")
    public static class Resource {

        @Path("/record")
        @GET
        public String beanParamRecord(BeanParamRecord p, @RestHeader String headerParam) {
            return p.headerParam() + "/"
                    + p.primitiveByte() + "/"
                    + p.primitiveShort() + "/"
                    + p.primitiveInt() + "/"
                    + p.primitiveLong() + "/"
                    + p.primitiveFloat() + "/"
                    + p.primitiveDouble() + "/"
                    + p.primitiveBoolean() + "/"
                    + p.primitiveChar() + "/"
                    + p.obp().q + "/"
                    + p.obp().obpr.q() + "/"
                    + p.obp().obpc.q + "/"
                    + p.obpr().q;
        }
    }
}
