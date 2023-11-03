package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(name = "exclusivedemo")
public class MutuallyExclusiveOptionsCommand implements Runnable {

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    Exclusive exclusive;

    @Override
    public void run() {
        System.out.println("-a:" + exclusive.a);
        System.out.println("-b:" + exclusive.b);
        System.out.println("-c:" + exclusive.c);
    }

    static class Exclusive {
        @CommandLine.Option(names = "-a", required = true)
        int a;
        @CommandLine.Option(names = "-b", required = true)
        int b;
        @CommandLine.Option(names = "-c", required = true)
        int c;
    }
}