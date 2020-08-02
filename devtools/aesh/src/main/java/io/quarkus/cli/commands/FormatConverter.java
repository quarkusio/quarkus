package io.quarkus.cli.commands;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.validator.OptionValidatorException;

public class FormatConverter implements Converter<ExtensionFormat, ConverterInvocation> {
    @Override
    public ExtensionFormat convert(ConverterInvocation converterInvocation) throws OptionValidatorException {
        if (converterInvocation.getInput() != null && converterInvocation.getInput().length() > 0)
            return ExtensionFormat.findFormat(converterInvocation.getInput());

        return ExtensionFormat.CONCISE;
    }
}
