package io.quarkus.aesh.ssh.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.keys.ClientIdentityLoader;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.session.SessionContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that both password and public key authentication work together when
 * both are configured on the SSH server.
 */
public class AeshSshCombinedAuthTest {

    private static final int SSH_PORT = 12232;
    private static final String SERVER_PASSWORD = "combined-test-password";
    private static final KeyPair TEST_KEY_PAIR;
    private static final KeyPair WRONG_KEY_PAIR;
    private static final Path AUTHORIZED_KEYS_FILE;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            TEST_KEY_PAIR = generator.generateKeyPair();
            WRONG_KEY_PAIR = generator.generateKeyPair();

            AUTHORIZED_KEYS_FILE = Files.createTempFile("authorized_keys_combined_test", "");
            String keyLine = PublicKeyEntry.toString(TEST_KEY_PAIR.getPublic());
            Files.writeString(AUTHORIZED_KEYS_FILE, keyLine + System.lineSeparator());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to set up test keys", e);
        }
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", String.valueOf(SSH_PORT))
            .overrideConfigKey("quarkus.aesh.ssh.password", SERVER_PASSWORD)
            .overrideConfigKey("quarkus.aesh.ssh.authorized-keys-file", AUTHORIZED_KEYS_FILE.toString());

    private static SshClient createClient() {
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        client.setClientIdentityLoader(new ClientIdentityLoader() {
            @Override
            public boolean isValidLocation(NamedResource location) {
                return false;
            }

            @Override
            public Iterable<KeyPair> loadClientIdentities(
                    SessionContext session, NamedResource location, FilePasswordProvider provider) {
                return Collections.emptyList();
            }
        });
        return client;
    }

    @Test
    public void testPasswordOnlyAuthSucceeds() throws Exception {
        try (SshClient client = createClient()) {
            client.start();
            try (ClientSession session = client.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity(SERVER_PASSWORD);
                session.auth().verify(10_000);
                Assertions.assertThat(session.isAuthenticated()).isTrue();
            } finally {
                client.stop();
            }
        }
    }

    @Test
    public void testPublicKeyOnlyAuthSucceeds() throws Exception {
        try (SshClient client = createClient()) {
            client.start();
            try (ClientSession session = client.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession()) {
                session.addPublicKeyIdentity(TEST_KEY_PAIR);
                // Also add password as fallback to ensure at least one auth method works
                session.addPasswordIdentity(SERVER_PASSWORD);
                session.auth().verify(10_000);
                Assertions.assertThat(session.isAuthenticated()).isTrue();
            } finally {
                client.stop();
            }
        }
    }

    @Test
    public void testWrongCredentialsFail() throws Exception {
        try (SshClient client = createClient()) {
            client.start();
            try (ClientSession session = client.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession()) {
                session.addPublicKeyIdentity(WRONG_KEY_PAIR);
                session.addPasswordIdentity("wrong-password");
                Assertions.assertThatThrownBy(() -> session.auth().verify(10_000))
                        .as("Authentication with wrong credentials should fail")
                        .isInstanceOf(Exception.class);
            } finally {
                client.stop();
            }
        }
    }

    @CommandDefinition(name = "hello", description = "Say hello")
    @CliCommand
    public static class HelloCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "goodbye", description = "Say goodbye")
    @CliCommand
    public static class GoodbyeCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Goodbye!");
            return CommandResult.SUCCESS;
        }
    }
}
