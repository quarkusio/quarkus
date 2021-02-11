//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-bom:999-MOCK@pom
//DEPS io.quarkus:quarkus-picocli

//JAVAC_OPTIONS -parameters
//
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {HelloCommand.class, GoodByeCommand.class})
public class GreetingCommand {
}

@CommandLine.Command(name = "hello", description = "Greet World!")
class HelloCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Hello World!");
    }
}

@CommandLine.Command(name = "goodbye", description = "Say goodbye to World!")
class GoodByeCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Goodbye World!");
    }
}
