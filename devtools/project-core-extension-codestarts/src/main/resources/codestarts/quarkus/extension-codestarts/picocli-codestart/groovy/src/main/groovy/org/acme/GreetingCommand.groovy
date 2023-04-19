package org.acme

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "greeting", mixinStandardHelpOptions = true)
class GreetingCommand implements Runnable {

    @Parameters(paramLabel = "<name>", defaultValue = "picocli",
        description = "Your name.")
    String name

    @Override
    void run() {
        println "Hello ${name}, go go commando!"
    }

}
