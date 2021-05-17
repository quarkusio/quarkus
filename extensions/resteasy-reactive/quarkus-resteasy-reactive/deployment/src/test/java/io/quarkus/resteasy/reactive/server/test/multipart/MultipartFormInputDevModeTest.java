package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.multipart.other.OtherPackageFormDataBase;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class MultipartFormInputDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FormDataBase.class, OtherPackageFormDataBase.class, FormData.class, Status.class,
                                    FormDataSameFileName.class, OtherFormData.class, OtherFormDataBase.class,
                                    MultipartResource.class);
                }

            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");
    private final File XML_FILE = new File("./src/test/resources/test.html");
    private final File TXT_FILE = new File("./src/test/resources/lorem.txt");

    @Test
    public void test() {
        doTest("simple");
        doTest("simple");

        TEST.modifySourceFile(MultipartResource.class.getSimpleName() + ".java", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("/simple", "/simple2");
            }
        });

        doTest("simple2");
        doTest("simple2");

        TEST.modifySourceFile(MultipartResource.class.getSimpleName() + ".java", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("/simple2", "/simple");
            }
        });

        doTest("simple");
        doTest("simple");
    }

    private void doTest(String path) {
        RestAssured.given()
                .multiPart("name", "Alice")
                .multiPart("active", "true")
                .multiPart("num", "25")
                .multiPart("status", "WORKING")
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("xmlFile", XML_FILE, "text/xml")
                .multiPart("txtFile", TXT_FILE, "text/plain")
                .accept("text/plain")
                .when()
                .post("/multipart/" + path + "/2")
                .then()
                .statusCode(200)
                .body(equalTo("Alice - true - 50 - WORKING - text/html - true - true"));
    }

}
