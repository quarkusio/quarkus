package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import io.quarkus.cli.build.BaseBuildCommand;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "extension", aliases = {
        "ext" }, sortOptions = false, mixinStandardHelpOptions = false, description = "List, add, and remove extensions of an existing project.", subcommands = {
                ProjectExtensionsAdd.class, ProjectExtensionsList.class, ProjectExtensionsRemove.class })
public class ProjectExtensions extends BaseBuildCommand implements Callable<Integer> {

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        output.info("No subcommand specified, listing current extensions (see --help).");

        ParseResult result = spec.commandLine().getParseResult();
        List<String> args = result.originalArgs().stream().filter(x -> !"extension".equals(x) && !"ext".equals(x))
                .collect(Collectors.toList());
        CommandLine listCommand = spec.subcommands().get("list");
        return listCommand.execute(args.toArray(new String[0]));
    }
}
