package io.quarkus.qute.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.qute.runtime.devmode.QuteErrorPageSetup;

@BuildSteps(onlyIf = IsDevelopment.class)
public class QuteDevModeProcessor {

    @BuildStep
    void collectGeneratedContents(EffectiveTemplatePathsBuildItem effectiveTemplatePaths,
            BuildProducer<ValidationErrorBuildItem> errors) {
        Map<String, String> contents = new HashMap<>();
        for (TemplatePathBuildItem template : effectiveTemplatePaths.getTemplatePaths()) {
            if (!template.isFileBased()) {
                contents.put(template.getPath(), template.getContent());
            }
        }
        // Set the global that could be used at runtime when a qute error page is rendered
        DevConsoleManager.setGlobal(QuteErrorPageSetup.GENERATED_CONTENTS, contents);
    }

    // This build step is only used to for a QuarkusDevModeTest that contains the QuteDummyTemplateGlobalMarker interface
    @BuildStep
    void generateTestTemplateGlobal(ApplicationIndexBuildItem applicationIndex,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanClasses) {
        if (applicationIndex.getIndex().getClassByName(
                DotName.createSimple("io.quarkus.qute.deployment.devmode.QuteDummyTemplateGlobalMarker")) != null) {
            // If the marker interface is present then we generate a dummy class annotated with @TemplateGlobal
            GeneratedBeanGizmoAdaptor gizmoAdaptor = new GeneratedBeanGizmoAdaptor(generatedBeanClasses,
                    new Predicate<String>() {
                        @Override
                        public boolean test(String t) {
                            return false;
                        }
                    });
            try (ClassCreator classCreator = ClassCreator.builder().className("org.acme.qute.test.QuteDummyGlobals")
                    .classOutput(gizmoAdaptor).build()) {
                classCreator.addAnnotation(TemplateGlobal.class);

                FieldCreator quteDummyFoo = classCreator.getFieldCreator("quteDummyFoo", String.class);
                quteDummyFoo.setModifiers(Modifier.STATIC);

                MethodCreator staticInitializer = classCreator.getMethodCreator("<clinit>", void.class);
                staticInitializer.setModifiers(Modifier.STATIC);
                staticInitializer.writeStaticField(quteDummyFoo.getFieldDescriptor(), staticInitializer.load("bar"));
                staticInitializer.returnVoid();
            }
        }
    }

}
