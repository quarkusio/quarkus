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
        // Configure JBoss LogManager
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        // Load logging configuration from classpath
        try {
            var configUrl = TlsCommand.class.getClassLoader().getResource("logging.properties");
            if (configUrl != null) {
                System.setProperty("logging.configuration", configUrl.toString());
            }
        } catch (Exception e) {
            // Ignore if configuration cannot be loaded
        }

        int exitCode = new CommandLine(new TlsCommand()).execute(args);
        System.exit(exitCode);
    }
}
