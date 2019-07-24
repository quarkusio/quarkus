package io.quarkus.kogito.deployment;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.kie.api.io.ResourceType;
import org.kie.kogito.codegen.ApplicationGenerator;
import org.kie.kogito.codegen.Generator;
import org.kie.kogito.codegen.rules.IncrementalRuleCodegen;

public class RulesCompilationProvider extends KogitoCompilationProvider {

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".drl");
    }

    @Override
    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        Collection<File> files = PackageWalker.getAllSiblings(filesToCompile);
        return appGen.withGenerator(
                IncrementalRuleCodegen.ofFiles(
                        files,
                        ResourceType.DRL))
                .withClassLoader(Thread.currentThread().getContextClassLoader())
                .withHotReloadMode();
    }

}
