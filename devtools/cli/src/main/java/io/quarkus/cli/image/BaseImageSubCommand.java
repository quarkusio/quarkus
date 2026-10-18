package io.quarkus.cli.image;

import java.util.Optional;

import io.quarkus.cli.common.BuildToolDelegatingCommand;
import picocli.CommandLine.ParentCommand;

public class BaseImageSubCommand extends BaseImageCommand {

    @ParentCommand
    BaseImageCommand parent;

    @Override
    public Optional<BuildToolDelegatingCommand> getParentCommand() {
        return Optional.of(parent);
    }
}
