package io.quarkus.it.liquibase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.LogFile;
import io.quarkus.test.QuarkusProdModeTest;

public class LiquibaseFunctionalityPMT {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(LiquibaseApp.class, LiquibaseFunctionalityResource.class)
                    .addAsResource("db")
                    .addAsResource("application.properties"))
            .setApplicationName("liquibase-prodmode-test")
            .setLogFileName("liquibase-prodmode-test.log")
            .setRun(true);

    @LogFile
    private Path logfile;

    @Test
    public void test() {
        LiquibaseFunctionalityTest.doTestLiquibaseQuarkusFunctionality();
    }

    @AfterEach
    void dumpLog() throws IOException {
        System.out.println(Files.readString(logfile));
    }
}
