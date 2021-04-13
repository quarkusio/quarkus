package ilove.quark.us.picocli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;
import java.net.URL;

@Command(name = "hello")
public class HelloCommand implements Runnable{

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    Name name;

    static class Name {

        @CommandLine.Option(names = {"--first-name"}, description = "The guest first name")
        String firstName;

        @CommandLine.Option(names = {"--nick-name"}, description = "The guest nickname")
        String nickname;

        String value(){
            return firstName != null ? firstName : nickname;
        }

    }

    private final GreetingService greetingService;

    public HelloCommand(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @Override
    public void run() {
        greetingService.sayHello(name.value());
    }
}
