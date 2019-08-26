package [=javaPackageBase].deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class [=artifactIdBaseCamelCase]ProcessorTest {

//    @RegisterExtension // Starting the Quarkus Application with your extension loaded
//    final static final QuarkusUnitTest config = new QuarkusUnitTest()
//        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
//                .addClasses(MyServlet, MyService.class) // Adding custom classes
//                .addAsResource("custom-application.properties", "application.properties") // Adding custom resources
//                .addAsResource("data.sql"));

//    @RegisterExtension // Starting the Quarkus Application with your extension loaded and hot reload enabled
//      final static QuarkusDevModeTest test = new QuarkusDevModeTest()
//          .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
//                 .addClasses(MyServlet, MyService.class) // Adding custom classes
//                 .addAsResource("custom-application.properties", "application.properties") // Adding custom resources
//                  .addAsResource("data.sql"));

//    @Test
//    public void testSomething() {
//        RestAssured.when().get("/").then() // You can easily test your HTTP enpoints with RestAssured
//                .statusCode(200).body(equalTo("Hello, World!"));
//    }

//    @Test
//    public void testHotRealoading() { // You can test you extension hot reloading strategy
//        RestAssured.when().get("/new").then()
//                .statusCode(404);
//        test.addSourceFile(NewServlet.class);
//
//        RestAssured.when().get("/new").then()
//                .statusCode(200).body(is("A new Servlet"));
//
//        test.modifySourceFile("NewServlet.java", (source) -> s.replace("A new Servlet", "Hello Quarkus"));
//
//        RestAssured.when().get("/new").then()
//                .statusCode(200).body(is("Hello Quarkus"));
//    }

}
