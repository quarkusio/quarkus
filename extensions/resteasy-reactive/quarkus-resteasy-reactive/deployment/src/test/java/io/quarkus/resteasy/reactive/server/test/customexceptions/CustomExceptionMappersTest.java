package io.quarkus.resteasy.reactive.server.test.customexceptions;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomExceptionMappersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FirstResource.class, SecondResource.class,
                                    MyException.class, MyOtherException.class, UniException.class, ExtendsUniException.class,
                                    MyOtherExceptionMapper.class, UniExceptionMapper.class,
                                    SomeBean.class, ExceptionUtil.class);
                }
            });

    @Test
    public void testResourceWithExceptionMapper() {
        RestAssured.get("/first?name=IllegalState")
                .then().statusCode(409);
        RestAssured.get("/first?name=IllegalArgument")
                .then().statusCode(409);
        RestAssured.get("/first?name=My")
                .then().statusCode(410).body(Matchers.equalTo("/first->throwsVariousExceptions"));
        RestAssured.get("/first?name=MyOther")
                .then().statusCode(411);
        RestAssured.get("/first?name=Uni")
                .then().statusCode(412).body(Matchers.equalTo("/first->throwsVariousExceptions"));
        RestAssured.get("/first?name=Other")
                .then().statusCode(500);
    }

    @Test
    public void testResourceWithExceptionMapperAndUniResponse() {
        RestAssured.get("/first/uni?name=IllegalState")
                .then().statusCode(409);
        RestAssured.get("/first/uni?name=IllegalArgument")
                .then().statusCode(409);
        RestAssured.get("/first/uni?name=My")
                .then().statusCode(410).body(Matchers.equalTo("/first/uni->uni"));
        RestAssured.get("/first/uni?name=MyOther")
                .then().statusCode(411);
        RestAssured.get("/first/uni?name=Uni")
                .then().statusCode(412).body(Matchers.equalTo("/first/uni->uni"));
        RestAssured.get("/first/uni?name=Other")
                .then().statusCode(500);
    }

    @Test
    public void testResourceWithoutExceptionMapper() {
        RestAssured.get("/second")
                .then().statusCode(500);
        RestAssured.get("/second/other")
                .then().statusCode(411);
        RestAssured.get("/second/uni")
                .then().statusCode(413);
        RestAssured.get("/second/extendsUni")
                .then().statusCode(414);
    }
}
