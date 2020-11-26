package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.GenericResourceCrudResource;
import io.quarkus.rest.server.test.resource.basic.resource.GenericResourceStudent;
import io.quarkus.rest.server.test.resource.basic.resource.GenericResourceStudentCrudResource;
import io.quarkus.rest.server.test.resource.basic.resource.GenericResourceStudentInterface;
import io.quarkus.rest.server.test.resource.basic.resource.GenericResourceStudentReader;
import io.quarkus.rest.server.test.resource.basic.resource.GenericResourceStudentWriter;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests generic resource class
 * @tpSince RESTEasy 3.0.20
 */
@DisplayName("Generic Resource Test")
public class GenericResourceTest {

    private static WebTarget proxy;

    @BeforeAll
    public static void setup() {
        WebTarget target = ClientBuilder.newClient().target(generateURL(""));
        proxy = target.register(GenericResourceStudentReader.class).register(GenericResourceStudentWriter.class);
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(GenericResourceStudent.class);
                    war.addClass(PortProviderUtil.class);
                    war.addClass(GenericResourceStudentInterface.class);
                    war.addClass(GenericResourceCrudResource.class);
                    war.addClasses(GenericResourceStudentCrudResource.class, GenericResourceStudentReader.class,
                            GenericResourceStudentWriter.class);
                    return war;
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, GenericResourceTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Get")
    @Disabled
    public void testGet() {
        //Assertions.assertTrue(proxy.get(1).getName().equals("Jozef Hartinger"));
    }

    @Test
    @DisplayName("Test Put")
    @Disabled
    public void testPut() {
        //proxy.put(2, new GenericResourceStudent("John Doe"));
        //Assertions.assertTrue(proxy.get(2).getName().equals("John Doe"));
    }
}
