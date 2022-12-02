package io.quarkus.avro.deployment;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class AvroCodeReloadTest {
    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(AvroReloadResource.class))
            .setCodeGenSources("avro")
            .setBuildSystemProperty("avro.codegen.avsc.imports", "imports");

    @Test
    void shouldAlterSchema() throws InterruptedException {
        assertThat(when().get().body().print().split(",")).containsExactlyInAnyOrder("Public", "Private");

        test.modifyFile("avro/imports/PrivacyImport.avsc",
                text -> text.replaceAll(Pattern.quote("\"symbols\" : [\"Public\",\"Private\"]"),
                        "\"symbols\" : [\"Public\",\"Private\",\"Default\"]"));
        Thread.sleep(5000); // to wait for eager reload for code gen sources to happen
        assertThat(when().get().body().print().split(",")).containsExactlyInAnyOrder("Public", "Private", "Default");
    }

    @Test
    void shouldAlterProtocol() throws InterruptedException {
        assertThat(when().get("/protocol").body().print().split(",")).containsExactlyInAnyOrder("Public", "Private");

        test.modifyFile("avro/User.avpr",
                text -> text.replaceAll(Pattern.quote("\"symbols\" : [ \"Public\", \"Private\"]"),
                        "\"symbols\" : [ \"Public\", \"Private\", \"Default\"]"));
        Thread.sleep(5000); // to wait for eager reload for code gen sources to happen
        assertThat(when().get("/protocol").body().print().split(",")).containsExactlyInAnyOrder("Public", "Private", "Default");
    }

    @Test
    void shouldAlterAvdl() throws InterruptedException {
        assertThat(when().get("/avdl").body().print().split(",")).containsExactlyInAnyOrder("LOW", "MEDIUM", "HIGH");

        test.modifyFile("avro/Hello.avdl",
                text -> text.replaceAll(Pattern.quote("LOW, MEDIUM, HIGH"),
                        "LOWER, MEDIUM, HIGHEST"));
        Thread.sleep(5000); // to wait for eager reload for code gen sources to happen
        assertThat(when().get("/avdl").body().print().split(",")).containsExactlyInAnyOrder("LOWER", "MEDIUM", "HIGHEST");
    }

}
