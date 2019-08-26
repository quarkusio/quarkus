package [=javaPackageBase].deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class [=artifactIdBaseCamelCase]ProcessorTest {

    // Starting the Quarkus Application
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
//                .addClasses(MyServlet, MyService.class) // Adding custom classes
//                .addAsResource("custom-application.properties", "application.properties") // Adding custom resources
//                .addAsResource("data.sql")
    );

//    @Test
//    public void testSomething() {
//        RestAssured.when().get("/").then() // You can easily test your HTTP enpoints with RestAssured
//                .statusCode(200).body(equalTo("Hello, World!"));
//    }
}
