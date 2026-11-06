package io.quarkus.analytics;

import static io.quarkus.analytics.ConfigService.ACCEPTANCE_PROMPT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.analytics.common.TestRestClient;
import io.quarkus.analytics.config.FileLocations;
import io.quarkus.analytics.config.TestFileLocationsImpl;
import io.quarkus.analytics.dto.config.NoopRemoteConfig;
import io.quarkus.analytics.dto.config.RemoteConfig;
import io.quarkus.analytics.util.FileUtils;
import io.quarkus.analytics.util.ForwardedInputStream;
import io.quarkus.analytics.util.InterruptibleLineReader;
import io.quarkus.devtools.messagewriter.MessageWriter;

class ConfigServiceTest {

    private FileLocations fileLocations;

    @BeforeEach
    void setUp() throws IOException {
        fileLocations = new TestFileLocationsImpl();
    }

    @AfterEach
    void tearDown() throws IOException {
        ((TestFileLocationsImpl) fileLocations).deleteAll();
    }

    @Test
    void activeWithRemoteConfig() throws IOException {
        RemoteConfig remoteConfig = RemoteConfig.builder()
                .active(true)
                .denyQuarkusVersions(Collections.emptyList())
                .denyUserIds(Collections.emptyList())
                .refreshInterval(Duration.ofHours(12)).build();
        FileUtils.write(remoteConfig, fileLocations.getRemoteConfigFile());
        long lastModified = fileLocations.getRemoteConfigFile().toFile().lastModified();

        ConfigService configService = createConfigService(remoteConfig);

        assertNotNull(configService);
        assertTrue(configService.isActive(), "Remote config is there and active");
        assertEquals(lastModified, fileLocations.getRemoteConfigFile().toFile().lastModified(), "File must not change");
    }

    @Test
    void inactiveNoQuestionAsked() throws IOException {
        deleteLocalConfigFile();

        ConfigService configService = createConfigService();

        assertNotNull(configService);
        assertFalse(configService.isActive());
    }

    @Test
    void inactiveUserAnsweredNo() throws IOException {
        deleteLocalConfigFile();
        FileUtils.append("{\"disabled\":true}", fileLocations.getLocalConfigFile());

        ConfigService configService = createConfigService();

        assertNotNull(configService);
        assertFalse(configService.isActive());
    }

    @Test
    void activeConfig() throws IOException {
        RemoteConfig remoteConfig = RemoteConfig.builder()
                .active(true)
                .denyQuarkusVersions(Collections.emptyList())
                .denyUserIds(Collections.emptyList())
                .refreshInterval(Duration.ZERO).build();

        assertFalse(Files.exists(fileLocations.getRemoteConfigFile()));

        ConfigService configService = new ConfigService(new TestRestClient(remoteConfig),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());

        assertNotNull(configService);
        assertTrue(configService.isActive());
        assertTrue(Files.exists(fileLocations.getRemoteConfigFile()));
    }

    @Test
    void remoteConfigOff() throws IOException {
        ConfigService configService = new ConfigService(new TestRestClient(NoopRemoteConfig.INSTANCE),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());

        assertNotNull(configService);
        assertFalse(configService.isActive());
    }

    @Test
    void isArtifactActive() throws IOException {
        ConfigService configService = new ConfigService(new TestRestClient(NoopRemoteConfig.INSTANCE),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());

        assertTrue(configService.isArtifactActive("allow.groupId",
                "allow.quarkus.version"));
        assertTrue(configService.isArtifactActive("allow.groupId",
                null));
        assertFalse(configService.isArtifactActive("",
                "allow.quarkus.version"));
        assertFalse(configService.isArtifactActive(null,
                null));
        assertFalse(configService.isArtifactActive("io.quarkus.opentelemetry",
                null));
    }

    @Test
    void isQuarkusVersionActive() throws IOException {
        AnonymousUserId userId = AnonymousUserId.getInstance(fileLocations, MessageWriter.info());
        RemoteConfig remoteConfig = RemoteConfig.builder()
                .active(true)
                .denyQuarkusVersions(List.of("deny.quarkus.version", "deny.quarkus.version2"))
                .denyUserIds(Collections.emptyList())
                .refreshInterval(Duration.ofHours(12)).build();
        ConfigService configService = new ConfigService(new TestRestClient(remoteConfig),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());

        assertTrue(configService.isArtifactActive("allow.groupId",
                "allow.quarkus.version"));
        assertFalse(configService.isArtifactActive("allow.groupId",
                "deny.quarkus.version"));
        assertFalse(configService.isArtifactActive("allow.groupId",
                "deny.quarkus.version2"));
    }

    @Test
    void isUserIsDisabled() throws IOException {
        AnonymousUserId userId = AnonymousUserId.getInstance(fileLocations, MessageWriter.info());
        RemoteConfig remoteConfig = RemoteConfig.builder()
                .active(true)
                .denyQuarkusVersions(Collections.emptyList())
                .denyUserIds(List.of(userId.getUuid()))
                .refreshInterval(Duration.ofHours(12)).build();
        ConfigService configService = new ConfigService(new TestRestClient(remoteConfig),
                userId,
                fileLocations,
                MessageWriter.info());

        assertFalse(configService.isUserEnabled(remoteConfig, userId.getUuid()));
        assertFalse(configService.isActive());
    }

    @Test
    void userAcceptance_alreadyAnswered() throws IOException {
        ConfigService configService = createConfigService();
        configService.userAcceptance(s -> {
            assertTrue(Files.exists(fileLocations.getLocalConfigFile()), "Local config file must be present");
            fail("User already answered");
            return "y";
        });
        assertTrue(Files.exists(fileLocations.getLocalConfigFile()), "Local config file must be present");
    }

    @Test
    void userAcceptance_yes() throws IOException {
        deleteLocalConfigFile();

        ConfigService configService = createConfigService();

        configService.userAcceptance(s -> {
            assertEquals(ACCEPTANCE_PROMPT, s);
            return "y";
        });

        assertTrue(configService.isActive());
        assertTrue(Files.exists(fileLocations.getLocalConfigFile()), "Local config file must be present");
        assertTrue(Files.exists(fileLocations.getRemoteConfigFile()), "remote config file was not created");
    }

    @Test
    void userAcceptance_no() throws IOException {
        deleteLocalConfigFile();

        ConfigService configService = createConfigService();

        configService.userAcceptance(s -> {
            assertEquals(ACCEPTANCE_PROMPT, s);
            return "n";
        });
        assertFalse(configService.isActive());
        assertTrue(Files.exists(fileLocations.getLocalConfigFile()), "Local config file must be present");
        assertFalse(Files.exists(fileLocations.getRemoteConfigFile()), "remote config file cannot be present");
    }

    @Test
    void userAcceptance_invalidInput() throws IOException {
        deleteLocalConfigFile();

        ConfigService configService = createConfigService();

        configService.userAcceptance(s -> {
            assertEquals(ACCEPTANCE_PROMPT, s);
            return "not valid input";
        });
        assertFalse(configService.isActive());
        assertFalse(Files.exists(fileLocations.getLocalConfigFile()), "Local config file cannot be present");
        assertFalse(Files.exists(fileLocations.getRemoteConfigFile()), "remote config file cannot be present");
    }

    @Test
    void userAcceptance_timeoutInterruptsPrompt() throws Exception {
        deleteLocalConfigFile();
        ConfigService configService = createConfigService();
        CountDownLatch promptInterrupted = new CountDownLatch(1);

        System.setProperty("quarkus.analytics.prompt.timeout", "0");
        try {
            configService.userAcceptance(s -> {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                } catch (InterruptedException e) {
                    promptInterrupted.countDown();
                }
                return "y";
            });
        } finally {
            System.clearProperty("quarkus.analytics.prompt.timeout");
        }

        assertTrue(promptInterrupted.await(5, TimeUnit.SECONDS),
                "The prompt must be interrupted once the timeout expires, so it stops reading the console");
        assertFalse(Files.exists(fileLocations.getLocalConfigFile()), "Local config file cannot be present");
    }

    @Test
    @Timeout(30)
    void userAcceptance_timedOutPromptDoesNotConsumeLaterInput() throws Exception {
        deleteLocalConfigFile();
        ConfigService configService = createConfigService();

        try (PipedOutputStream console = new PipedOutputStream();
                PipedInputStream consoleInput = new PipedInputStream(console)) {
            System.setProperty("quarkus.analytics.prompt.timeout", "0");
            try {
                configService.userAcceptance(s -> {
                    try {
                        return InterruptibleLineReader.readLine(consoleInput);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } finally {
                System.clearProperty("quarkus.analytics.prompt.timeout");
            }

            // a key pressed for the dev mode console after the prompt gave up must reach the dev mode console
            console.write('h');
            console.flush();
            // leave a prompt that is still polling the console plenty of time to steal the key
            Thread.sleep(200);
            assertEquals(1, consoleInput.available(), "The timed out prompt consumed console input");
            assertEquals('h', consoleInput.read());
        }
        assertFalse(Files.exists(fileLocations.getLocalConfigFile()), "Local config file cannot be present");
    }

    @Test
    @Timeout(30)
    void userAcceptance_timedOutPromptDoesNotConsumeLaterForwardedInput() throws Exception {
        deleteLocalConfigFile();
        ConfigService configService = createConfigService();
        ForwardedInputStream console = new ForwardedInputStream();

        System.setProperty("quarkus.analytics.prompt.timeout", "0");
        try {
            configService.userAcceptance(s -> {
                try {
                    return InterruptibleLineReader.readLineInterruptibly(console);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } finally {
            System.clearProperty("quarkus.analytics.prompt.timeout");
        }

        // a line typed for the dev mode console after the prompt gave up must reach the dev mode console
        console.type("h\n");
        // leave a prompt that is still reading the console plenty of time to steal the line
        Thread.sleep(200);
        assertEquals('h', console.read(), "The timed out prompt consumed console input");
        assertFalse(Files.exists(fileLocations.getLocalConfigFile()), "Local config file cannot be present");
    }

    @Test
    void noRemoteConfigOnInit() throws IOException {
        deleteLocalConfigFile();
        assertFalse(Files.exists(fileLocations.getLocalConfigFile()), "Local config file cannot be present");
        assertFalse(Files.exists(fileLocations.getRemoteConfigFile()), "remote config file cannot be present");
        ConfigService configService = new ConfigService(new TestRestClient(NoopRemoteConfig.INSTANCE),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());
        assertFalse(configService.isActive());
        assertFalse(Files.exists(fileLocations.getLocalConfigFile()), "Local config file cannot be present on 1st init");
        assertFalse(Files.exists(fileLocations.getRemoteConfigFile()), "remote config file cannot be present on 1st init");
    }

    @Test
    void noRemoteConfigUserDisabledByProp() throws IOException {
        deleteLocalConfigFile();
        System.setProperty("quarkus.analytics.disabled", "true");
        ConfigService configService = createConfigService();

        configService.userAcceptance(s -> {
            fail("User should not be asked");
            return null;
        });

        assertFalse(configService.isActive());
        assertFalse(Files.exists(fileLocations.getLocalConfigFile()), "Local config is present");
        assertFalse(Files.exists(fileLocations.getRemoteConfigFile()), "remote config file cannot be present on 1st init");
        System.clearProperty("quarkus.analytics.disabled");
    }

    private void deleteLocalConfigFile() {
        fileLocations.getLocalConfigFile().toFile().delete();
    }

    private ConfigService createConfigService() throws IOException {
        RemoteConfig remoteConfig = RemoteConfig.builder()
                .active(true)
                .denyQuarkusVersions(Collections.emptyList())
                .denyUserIds(Collections.emptyList())
                .refreshInterval(Duration.ofHours(12)).build();
        return createConfigService(remoteConfig);
    }

    private ConfigService createConfigService(final RemoteConfig remoteConfig) throws IOException {
        ConfigService configService = new ConfigService(new TestRestClient(remoteConfig),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());
        return configService;
    }
}
