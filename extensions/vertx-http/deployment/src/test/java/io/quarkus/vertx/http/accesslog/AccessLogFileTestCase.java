/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.vertx.http.accesslog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.http.ProtocolVersion;
import org.apache.http.params.CoreProtocolPNames;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;

/**
 * Tests writing the access log to a file
 *
 * @author Stuart Douglas
 */
public class AccessLogFileTestCase {

    @RegisterExtension
    public static QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    Path logDirectory;
                    try {
                        logDirectory = Files.createTempDirectory("quarkus-tests");
                        //backslash is an escape char, we need this to be properly formatted for windows
                        Properties p = new Properties();
                        p.setProperty("quarkus.http.access-log.enabled", "true");
                        p.setProperty("quarkus.http.access-log.log-to-file", "true");
                        p.setProperty("quarkus.http.access-log.base-file-name", "server");
                        p.setProperty("quarkus.http.access-log.log-directory", logDirectory.toAbsolutePath().toString());
                        p.setProperty("quarkus.http.access-log.pattern", "long");
                        p.setProperty("quarkus.http.access-log.exclude-pattern", "/health|/liveliness");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        p.store(out, null);

                        return ShrinkWrap.create(JavaArchive.class)
                                .add(new ByteArrayAsset(out.toByteArray()),
                                        "application.properties");

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

    @ConfigProperty(name = "quarkus.http.access-log.log-directory")
    Path logDirectory;

    @BeforeEach
    public void before() throws IOException {
        Files.createDirectories(logDirectory);
    }

    @AfterEach
    public void after() throws IOException {
        IoUtils.recursiveDelete(logDirectory);
    }

    @Test
    public void testSingleLogMessageToFile() throws IOException, InterruptedException {
        // issue the request with a specific HTTP protocol version, so that we can then verify
        // the protocol value logged in the access log file
        final RestAssuredConfig http10Config = RestAssured.config().httpClient(
                new HttpClientConfig().setParam(CoreProtocolPNames.PROTOCOL_VERSION, new ProtocolVersion("HTTP", 1, 0)));
        final RequestSpecification requestSpec = new RequestSpecBuilder().setConfig(http10Config).build();
        final String paramValue = UUID.randomUUID().toString();
        RestAssured.given(requestSpec).get("/health"); //should be ignored
        RestAssured.given(requestSpec).get("/liveliness"); //should be ignored
        RestAssured.given(requestSpec).get("/does-not-exist?foo=" + paramValue);

        Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        try (Stream<Path> files = Files.list(logDirectory)) {
                            Assertions.assertEquals(1, (int) files.count());
                        }
                        Path path = logDirectory.resolve("server.log");
                        Assertions.assertTrue(Files.exists(path));
                        String data = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                        Assertions.assertFalse(data.contains("/health"));
                        Assertions.assertFalse(data.contains("/liveliness"));
                        Assertions.assertTrue(data.contains("/does-not-exist"));
                        Assertions.assertTrue(data.contains("?foo=" + paramValue),
                                "access log is missing query params");
                        Assertions.assertFalse(data.contains("?foo=" + paramValue + "?foo=" + paramValue),
                                "access log contains duplicated query params");
                        Assertions.assertTrue(data.contains("HTTP/1.0"),
                                "HTTP/1.0 protocol value is missing in the access log");
                        Assertions.assertTrue(data.contains("Accept: */*"),
                                "Accept header is missing in the access log");
                    }
                });
    }

}
