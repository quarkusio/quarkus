package io.quarkus.cli.commands;

import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.validator.ValidatorInvocation;

public class DevModeDebugValidator implements OptionValidator<ValidatorInvocation<String, DevModeCommand>> {
    @Override
    public void validate(ValidatorInvocation<String, DevModeCommand> invocation) throws OptionValidatorException {
        if (!invocation.getValue().equals("true") ||
                !invocation.getValue().equals("false") ||
                !invocation.getValue().equals("client")) {
            //finally check if the value is a number
            try {
                Integer.parseInt(invocation.getValue());
            } catch (NumberFormatException nfe) {
                throw new OptionValidatorException("The debug option need to be either: true, false, client or a port number");
            }
        }

    }
}
