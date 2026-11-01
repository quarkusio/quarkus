package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "extension", aliases = {
        "ext" }, header = "Configure extensions of an existing project.", subcommands = {
                ProjectExtensionsList.class,
                ProjectExtensionsCategories.class,
                ProjectExtensionsAdd.class,
                ProjectExtensionsRemove.class })
public class ProjectExtensions implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        return DefaultSubcommand.forward(spec, output, "list", "Listing extensions (default action, see --help).");
    }
}
