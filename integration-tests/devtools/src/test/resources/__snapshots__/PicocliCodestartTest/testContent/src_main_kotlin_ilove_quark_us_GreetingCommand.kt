package ilove.quark.us

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "greeting", mixinStandardHelpOptions = true)
class GreetingCommand : Runnable {

    @Parameters(paramLabel = "<name>", defaultValue = "picocli", description = ["Your name."])
    var name: String? = null
    override fun run() {
        System.out.printf("Hello %s, go go commando!\n", name)
    }

}