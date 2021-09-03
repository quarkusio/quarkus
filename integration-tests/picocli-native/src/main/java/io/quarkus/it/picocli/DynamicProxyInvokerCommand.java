package io.quarkus.it.picocli;

import javax.inject.Inject;

import picocli.CommandLine;

@CommandLine.Command(name = "dynamic-proxy")
public class DynamicProxyInvokerCommand implements Runnable {
    @Inject
    CommandLine.IFactory factory;

    @Override
    public void run() {
        CommandLine cmd = new CommandLine(DynamicProxyCommand.class, factory);
        cmd.parseArgs("-t", "2007-12-03T10:15:30");
        DynamicProxyCommand parsedCommand = cmd.getCommand();
        System.out.println(parsedCommand.time());
    }
}
