package io.quarkus.it.picocli;

import java.util.Arrays;
import java.util.Iterator;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "completion-reflection")
public class CompletionReflectionCommand implements Runnable {

    @Parameters(paramLabel = "parameter", completionCandidates = ConnectorCompletions.class)
    String parameter;

    @Override
    public void run() {
    }

    private static class ConnectorCompletions implements Iterable<String> {

        @Override
        public Iterator<String> iterator() {
            return Arrays.asList("one", "two").iterator();
        }
    }
}
