//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11
//JAVAC_OPTIONS -parameters
//DEPS io.quarkus:quarkus-bom:999-MOCK@pom
//DEPS io.quarkus:quarkus-picocli

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "Greeting", mixinStandardHelpOptions = true)
public class main implements Runnable {

    @Parameters(paramLabel = "<name>", defaultValue = "picocli",
        description = "Your name.")
    String name;

    @Override
    public void run() {
        System.out.printf("Hello %s, go go commando!%n", name);
    }
}
