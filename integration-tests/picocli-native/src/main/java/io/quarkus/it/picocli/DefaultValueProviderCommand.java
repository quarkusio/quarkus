package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(name = "defaultvalueprovider", mixinStandardHelpOptions = true, defaultValueProvider = CustomDefaultValueProvider.class)
public class DefaultValueProviderCommand implements Runnable {

    @Override
    public void run() {
    }

}
