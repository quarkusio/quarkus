package io.quarkus.it.picocli;

import java.util.Arrays;
import java.util.List;

import picocli.CommandLine;

@CommandLine.Command(name = "unmatched")
public class UnmatchedCommand implements Runnable {
    @CommandLine.Option(names = "-a")
    String alpha;
    @CommandLine.Option(names = "-b")
    String beta;
    @CommandLine.Parameters
    String[] remainder;
    @CommandLine.Unmatched
    List<String> unmatched;

    @Override
    public void run() {
        System.out.println("-a:" + alpha);
        System.out.println("-b:" + beta);
        System.out.println("remainder:" + Arrays.toString(remainder));
        System.out.println("unmatched" + unmatched);
    }
}
