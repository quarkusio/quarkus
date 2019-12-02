package io.quarkus.it.mongodb;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.IOException;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@DisabledOnOs(OS.LINUX)
//No SSL support for Linux as documented here https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/233
@QuarkusTest
class SSLMongoTest {

    private static final Logger LOGGER = Logger.getLogger(SSLMongoTest.class);
    private static MongodExecutable MONGO;

    @BeforeAll
    public static void startMongoDatabase() throws IOException {

        ClassLoader classLoader = SSLMongoTest.class.getClassLoader();
        File file = new File(classLoader.getResource("server.pem").getFile());

        Version.Main version = Version.Main.V4_0;
        int port = 27018;
        LOGGER.infof("Starting Mongo %s on port %s", version, port);
        IMongodConfig config = new MongodConfigBuilder()
                .version(version)
                .net(new Net(port, Network.localhostIsIPv6()))
                .withLaunchArgument("--sslMode", "preferSSL")
                .withLaunchArgument("--sslPEMKeyFile", file.getAbsolutePath())
                .build();
        MONGO = MongodStarter.getDefaultInstance().prepare(config);
        MONGO.start();
    }

    @AfterAll
    public static void stopMongoDatabase() {
        if (MONGO != null) {
            MONGO.stop();
        }
    }

    @Test
    public void testConnection() {
        RestAssured.when().get("/connectionStatus").then().statusCode(200).body(is("1.0"));
    }

}
