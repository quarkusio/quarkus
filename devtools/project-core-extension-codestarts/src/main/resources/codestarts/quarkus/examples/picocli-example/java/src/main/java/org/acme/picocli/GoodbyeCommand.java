package org.acme.picocli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "goodbye")
public class GoodbyeCommand implements Runnable {

    @Option(names = {"--name"}, description = "Guest name")
    String name;

    @Option(names = {"--times", "-t"}, defaultValue = "1", description = "How many time should we say goodbye")
    int times;

    @Override
    public void run() {
        for (int i = 0;i<times;i++) {
            System.out.printf("Goodbye %s!\n", name);
        }
    }
}
