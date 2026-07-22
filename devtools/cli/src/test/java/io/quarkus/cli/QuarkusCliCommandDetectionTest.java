package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;

public class QuarkusCliCommandDetectionTest {

    private QuarkusCli cli;
    private CommandLine root;

    @BeforeEach
    void setUp() {
        cli = new QuarkusCli();
        root = new CommandLine(cli);
    }

    @Test
    void extractCommandPathStopsAtFlags() {
        assertThat(QuarkusCli.extractCommandPath(new String[] { "update", "--update-recipes-version=x" }))
                .containsExactly("update");
        assertThat(QuarkusCli.extractCommandPath(new String[] { "create", "my-project", "-x" }))
                .containsExactly("create", "my-project");
        assertThat(QuarkusCli.extractCommandPath(new String[] { "config", "encrypt", "secret", "--key=k" }))
                .containsExactly("config", "encrypt", "secret");
    }

    @Test
    void createWithProjectNameIsNotMissingCommand() {
        // Hybrid catch-all: create my-project must not look for plugin create-my-project (#46645)
        assertThat(cli.findMissingCommand(root, new String[] { "create", "my-project" })).isEmpty();
    }

    @Test
    void existingCommandWithUnknownOptionIsNotMissingCommand() {
        // Options are ignored for command resolution so Picocli reports the real error (#45922)
        assertThat(cli.findMissingCommand(root, new String[] { "update", "--update-recipes-version=999-SNAPSHOT" }))
                .isEmpty();
    }

    @Test
    void leafCommandParametersAreNotMissingCommands() {
        // Positional args of encrypt/decrypt are parameters, not missing subcommands
        assertThat(cli.findMissingCommand(root,
                new String[] { "config", "decrypt", "SECRET", "KEY", "-f=plain" })).isEmpty();
        assertThat(cli.findMissingCommand(root, new String[] { "config", "encrypt", "12345678" })).isEmpty();
    }

    @Test
    void unknownCommandIsReportedAsMissing() {
        Optional<String> missing = cli.findMissingCommand(root, new String[] { "definitely-not-a-command", "arg" });
        assertThat(missing).contains("definitely-not-a-command");
    }

    @Test
    void unknownSubcommandIsReportedAsMissing() {
        Optional<String> missing = cli.findMissingCommand(root, new String[] { "config", "nope", "extra" });
        assertThat(missing).contains("config-nope");
    }

    @Test
    void extractCommandPathHandlesEndOfOptions() {
        List<String> path = QuarkusCli.extractCommandPath(new String[] { "config", "encrypt", "--", "-looks-like-flag" });
        assertThat(path).containsExactly("config", "encrypt", "-looks-like-flag");
    }
}
