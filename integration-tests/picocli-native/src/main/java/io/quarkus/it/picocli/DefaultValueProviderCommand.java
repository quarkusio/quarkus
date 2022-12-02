package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(name = "default-value-provider", mixinStandardHelpOptions = false, defaultValueProvider = CustomDefaultValueProvider.class)
public class DefaultValueProviderCommand implements Runnable {

    @CommandLine.Option(names = "--defaulted")
    String defaulted;

    @Override
    public void run() {
        System.out.println("default:" + defaulted);
    }

}
