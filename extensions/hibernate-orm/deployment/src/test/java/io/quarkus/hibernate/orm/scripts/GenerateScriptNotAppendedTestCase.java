package io.quarkus.hibernate.orm.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusDevModeTest;

public class GenerateScriptNotAppendedTestCase {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-generate-script.properties", "application.properties")
                    .addClasses(MyEntity.class));

    @RepeatedTest(2)
    public void verifyScriptIsOverwritten() throws Exception {
        String script = Files.readString(Path.of(GenerateScriptNotAppendedTestCase.class.getResource("/create.sql").toURI()));
        assertEquals(1, Pattern.compile("create table MyEntity").matcher(script).results().count());
    }

}
