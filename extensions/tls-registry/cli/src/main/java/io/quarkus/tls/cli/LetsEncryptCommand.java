package io.quarkus.tls.cli;

import io.quarkus.tls.cli.letsencrypt.LetsEncryptIssueCommand;
import io.quarkus.tls.cli.letsencrypt.LetsEncryptPrepareCommand;
import io.quarkus.tls.cli.letsencrypt.LetsEncryptRenewCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "lets-encrypt", mixinStandardHelpOptions = true, sortOptions = false, header = "Prepare, generate and renew Let's Encrypt Certificates", subcommands = {
        LetsEncryptPrepareCommand.class,
        LetsEncryptIssueCommand.class,
        LetsEncryptRenewCommand.class,
})
public class LetsEncryptCommand {

}
