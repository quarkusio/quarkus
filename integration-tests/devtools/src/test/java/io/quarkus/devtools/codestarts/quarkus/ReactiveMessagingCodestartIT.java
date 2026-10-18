package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import io.quarkus.maven.dependency.ArtifactKey;

public class ReactiveMessagingCodestartIT {

    @RegisterExtension
    public static QuarkusCodestartTest kafkaCodestartTest = QuarkusCodestartTest.builder()
            .extension(ArtifactKey.fromString("io.quarkus:quarkus-messaging-kafka"))
            .languages(JAVA)
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest amqpCodestartTest = QuarkusCodestartTest.builder()
            .extension(ArtifactKey.fromString("io.quarkus:quarkus-messaging-amqp"))
            .languages(JAVA)
            .build();

    @Test
    void testKafkaContent() throws Throwable {
        kafkaCodestartTest.checkGeneratedSource("org.acme.MyMessagingApplication");
        kafkaCodestartTest.checkGeneratedTestSource("org.acme.MyMessagingApplicationTest");
        kafkaCodestartTest.assertThatGeneratedFileMatchSnapshot(JAVA, "src/main/resources/application.properties");
    }

    @Test
    void testAMQPContent() throws Throwable {
        amqpCodestartTest.checkGeneratedSource("org.acme.MyMessagingApplication");
        amqpCodestartTest.checkGeneratedTestSource("org.acme.MyMessagingApplicationTest");
        amqpCodestartTest.assertThatGeneratedFileMatchSnapshot(JAVA, "src/main/resources/application.properties");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void buildKafka() throws Throwable {
        kafkaCodestartTest.buildAllProjects();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void buildAMQP() throws Throwable {
        amqpCodestartTest.buildAllProjects();
    }
}
