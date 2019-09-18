package io.quarkus.kogito.deployment;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.kie.kogito.codegen.ApplicationGenerator;
import org.kie.kogito.codegen.Generator;
import org.kie.kogito.codegen.process.ProcessCodegen;

public class ProcessCompilationProvider extends KogitoCompilationProvider {

    @Override
    public Set<String> handledExtensions() {
        return new HashSet<>(asList(".bpmn", ".bpmn2"));
    }

    @Override
    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        return appGen.withGenerator(
                ProcessCodegen.ofFiles(new ArrayList<>(filesToCompile)))
                .withClassLoader(Thread.currentThread().getContextClassLoader());
    }
}
