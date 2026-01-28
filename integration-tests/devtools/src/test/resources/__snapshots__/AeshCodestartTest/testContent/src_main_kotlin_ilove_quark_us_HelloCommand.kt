package ilove.quark.us

import org.aesh.command.Command
import org.aesh.command.CommandDefinition
import org.aesh.command.CommandResult
import org.aesh.command.invocation.CommandInvocation
import org.aesh.command.option.Option

@CommandDefinition(name = "hello", description = "Greet someone")
class HelloCommand : Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", description = "Your name.",
            defaultValue = ["aesh"])
    var name: String? = null

    override fun execute(invocation: CommandInvocation): CommandResult {
        invocation.println("Hello $name, go go commando!")
        return CommandResult.SUCCESS
    }

}
