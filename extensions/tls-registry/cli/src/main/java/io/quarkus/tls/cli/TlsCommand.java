package io.quarkus.tls.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "tls", mixinStandardHelpOptions = true, sortOptions = false, header = "Install and Manage TLS development certificates", subcommands = {
        GenerateCACommand.class,
        GenerateCertificateCommand.class,
        LetsEncryptCommand.class
})
public class TlsCommand implements Callable<Integer> {

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TlsCommand()).execute(args);
        System.exit(exitCode);
    }
}
