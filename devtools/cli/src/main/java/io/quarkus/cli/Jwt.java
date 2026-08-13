package io.quarkus.cli;

import io.quarkus.cli.jwt.GenerateKey;
import picocli.CommandLine.Command;

@Command(name = "jwt", sortOptions = false, header = "JWT key management.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "Options:%n", subcommands = {
        GenerateKey.class })
public class Jwt extends BaseBuildCommand implements Runnable {

    @Override
    public void run() {
        spec.commandLine().usage(output.out());
    }

}
