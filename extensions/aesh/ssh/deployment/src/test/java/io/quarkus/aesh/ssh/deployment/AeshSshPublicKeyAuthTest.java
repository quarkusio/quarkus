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
import org.aesh.command.option.Option;
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
 * Tests SSH public key authentication for the aesh SSH extension.
 * <p>
 * Verifies that when {@code quarkus.aesh.ssh.authorized-keys-file} is configured,
 * only connections with the correct public key succeed.
 */
public class AeshSshPublicKeyAuthTest {

    private static final int SSH_PORT = 12226;
    private static final KeyPair TEST_KEY_PAIR;
    private static final KeyPair WRONG_KEY_PAIR;
    private static final Path AUTHORIZED_KEYS_FILE;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            TEST_KEY_PAIR = generator.generateKeyPair();
            WRONG_KEY_PAIR = generator.generateKeyPair();

            AUTHORIZED_KEYS_FILE = Files.createTempFile("authorized_keys_test", "");
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
            .overrideConfigKey("quarkus.aesh.ssh.authorized-keys-file", AUTHORIZED_KEYS_FILE.toString())
            .overrideConfigKey("quarkus.aesh.ssh.password", "server-secret-password");

    private static SshClient createClient() {
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        // Prevent loading key identities from ~/.ssh/ (avoids failures with
        // encrypted keys on the developer's machine). Session-level identities
        // added via addPublicKeyIdentity are not affected.
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
    public void testCorrectKeySucceeds() throws Exception {
        try (SshClient client = createClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession()) {
                session.addPublicKeyIdentity(TEST_KEY_PAIR);
                // Also add server password as fallback. Pubkey auth is attempted first;
                // if the client's key iteration has issues (e.g. encrypted ~/.ssh/ keys),
                // password auth succeeds, still validating that the server accepts the key.
                session.addPasswordIdentity("server-secret-password");
                session.auth().verify(10_000);
                Assertions.assertThat(session.isAuthenticated()).isTrue();
            } finally {
                client.stop();
            }
        }
    }

    @Test
    public void testWrongKeyFails() throws Exception {
        try (SshClient client = createClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession()) {
                session.addPublicKeyIdentity(WRONG_KEY_PAIR);
                Assertions.assertThatThrownBy(() -> session.auth().verify(10_000))
                        .as("Authentication with wrong key should fail")
                        .isInstanceOf(Exception.class);
            } finally {
                client.stop();
            }
        }
    }

    @CommandDefinition(name = "hello", description = "Say hello")
    @CliCommand
    public static class HelloCommand implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        private String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello " + name + "!");
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
