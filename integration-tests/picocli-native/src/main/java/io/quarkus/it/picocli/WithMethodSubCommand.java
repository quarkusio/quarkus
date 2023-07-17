package io.quarkus.it.picocli;

import org.jboss.logging.Logger;

import picocli.CommandLine;

@CommandLine.Command(name = "with-method-sub-command")
public class WithMethodSubCommand {

    private final static Logger LOGGER = Logger.getLogger(WithMethodSubCommand.class);

    @CommandLine.Command
    void hello(@CommandLine.Option(names = { "-n", "--names" }, description = "Parameter option") String name) {
        System.out.println("Hello " + name);
    }

    @CommandLine.Command
    void loggingHello(@CommandLine.Option(names = { "-n", "--names" }, description = "Parameter option") String name) {
        LOGGER.error("Hello " + name);
    }

    @CommandLine.Command
    void goodBye(@CommandLine.Mixin NameMixin mixin) {
        System.out.println("Goodbye " + mixin.name);
    }
}
