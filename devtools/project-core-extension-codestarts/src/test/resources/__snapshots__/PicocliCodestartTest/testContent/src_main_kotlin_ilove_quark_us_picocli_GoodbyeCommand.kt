package ilove.quark.us.picocli

import picocli.CommandLine

@CommandLine.Command(name = "goodbye")
class GoodbyeCommand : Runnable {
    @CommandLine.Option(names = ["--name"], description = ["Guest name"])
    var name: String? = null

    @CommandLine.Option(names = ["--times", "-t"], defaultValue = "1", description = ["How many time should we say goodbye"])
    var times = 0
    override fun run() {
        for (i in 0 until times) {
            System.out.printf("Goodbye %s!\n", name)
        }
    }
}
