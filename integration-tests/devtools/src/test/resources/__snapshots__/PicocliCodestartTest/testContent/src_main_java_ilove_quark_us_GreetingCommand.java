package ilove.quark.us;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "greeting", mixinStandardHelpOptions = true)
public class GreetingCommand implements Runnable {

    @Parameters(paramLabel = "<name>", defaultValue = "picocli",
        description = "Your name.")
    String name;

    @Override
    public void run() {
        System.out.printf("Hello %s, go go commando!\n", name);
    }

    // Use in IDE: Starts the app for development. Not used in production.
    public static void main(String... args) { io.quarkus.runtime.Quarkus.run(args); }

}
