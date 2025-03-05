package io.quarkus.resteasy.reactive.server.test.beanparam;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class BeanParamRecordDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(FirstAndSecondResource.class,
                            FirstAndSecondResource.Param.class);
                }
            });

    @Test
    void test() {
        when().get("fs/foo/bar")
                .then()
                .statusCode(200)
                .body(is("foo-bar"));

        TEST.modifySourceFile(FirstAndSecondResource.class, (orig) -> orig.replace("-", "#"));

        when().get("fs/foo/bar")
                .then()
                .statusCode(200)
                .body(is("foo#bar"));
    }

}
