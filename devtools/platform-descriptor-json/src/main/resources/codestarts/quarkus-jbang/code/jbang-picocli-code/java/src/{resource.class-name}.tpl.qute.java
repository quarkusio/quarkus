//usr/bin/env jbang "$0" "$@" ; exit $?
{#for dep in dependencies}
//DEPS {dep.formatted-ga}:{quarkus.version}
{/for}

//JAVAC_OPTIONS -parameters
//
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {HelloCommand.class, GoodByeCommand.class})
public class EntryCommand {
}

@CommandLine.Command(name = "{resource.hello-name}", description = "{resource.hello-description}")
class HelloCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("{resource.hello-message");
    }
}

@CommandLine.Command(name = "{resource.goodbye-name}", description = "{resource.goodbye-description}")
class GoodByeCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("{resource.goodbye-message}");
    }
}
