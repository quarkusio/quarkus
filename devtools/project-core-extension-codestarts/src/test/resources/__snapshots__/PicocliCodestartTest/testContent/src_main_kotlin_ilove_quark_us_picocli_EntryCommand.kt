package ilove.quark.us.picocli

import io.quarkus.picocli.runtime.annotations.TopCommand
import picocli.CommandLine

@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = [HelloCommand::class, GoodbyeCommand::class])
class EntryCommand 
