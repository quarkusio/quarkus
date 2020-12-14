//usr/bin/env jbang "$0" "$@" ; exit $?
{#for dep in dependencies}
//DEPS {dep.formatted-ga}:{quarkus.version}
{/for}

//JAVAC_OPTIONS -parameters
//
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {command.subcommands})
public class {command.class-name} {
}

@CommandLine.Command(name = "{command.hello.name}", description = "{command.hello.description}")
class HelloCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("{command.hello.message}");
    }
}

@CommandLine.Command(name = "{command.goodbye.name}", description = "{command.goodbye.description}")
class GoodByeCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("{command.goodbye.message}");
    }
}
