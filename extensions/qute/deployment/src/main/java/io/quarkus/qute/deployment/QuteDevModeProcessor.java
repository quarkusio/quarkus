package io.quarkus.qute.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.qute.runtime.devmode.QuteErrorPageSetup;

@BuildSteps(onlyIf = IsDevelopment.class)
public class QuteDevModeProcessor {

    @BuildStep
    void collectGeneratedContents(List<TemplatePathBuildItem> templatePaths,
            BuildProducer<ValidationErrorBuildItem> errors) {
        Map<String, String> contents = new HashMap<>();
        for (TemplatePathBuildItem template : templatePaths) {
            if (!template.isFileBased()) {
                contents.put(template.getPath(), template.getContent());
            }
        }
        // Set the global that could be used at runtime when a qute error page is rendered
        DevConsoleManager.setGlobal(QuteErrorPageSetup.GENERATED_CONTENTS, contents);
    }

}
