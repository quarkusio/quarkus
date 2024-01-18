package io.quarkus.it.picocli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class EntryWithVersionCommand {
}
