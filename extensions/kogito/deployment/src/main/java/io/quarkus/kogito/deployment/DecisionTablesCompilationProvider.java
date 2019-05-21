package io.quarkus.kogito.deployment;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.kie.api.io.ResourceType;
import org.kie.kogito.codegen.ApplicationGenerator;
import org.kie.kogito.codegen.Generator;
import org.kie.kogito.codegen.rules.IncrementalRuleCodegen;

public class DecisionTablesCompilationProvider extends KogitoCompilationProvider {

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".xls");
    }

    @Override
    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        return new IncrementalRuleCodegen(context.getOutputDirectory().toPath().getParent().getParent(), filesToCompile,
                ResourceType.DTABLE);
    }
}
