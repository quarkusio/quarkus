package io.quarkus.cli.commands;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.validator.OptionValidatorException;

import io.quarkus.devtools.project.BuildTool;

public class BuildToolConverter implements Converter<BuildTool, ConverterInvocation> {
    @Override
    public BuildTool convert(ConverterInvocation invocation) throws OptionValidatorException {
        if (invocation.getInput() != null && invocation.getInput().length() > 0)
            return BuildTool.findTool(invocation.getInput());
        else
            throw new OptionValidatorException(invocation.getInput() + " was not recognized as a supported build tool");
    }
}
