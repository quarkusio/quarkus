package io.quarkus.azure.functions.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OverridePackageConfigBuildItem;

public class AzureFunctionsProcessor {
    private static final Logger log = Logger.getLogger(AzureFunctionsProcessor.class);

    @BuildStep
    public OverridePackageConfigBuildItem forceLegacy(PackageConfig config) {
        // Azure Functions need a legacy jar and no runner
        config.addRunnerSuffix = false;
        config.type = PackageConfig.BuiltInType.LEGACY_JAR.name();
        return new OverridePackageConfigBuildItem();
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AZURE_FUNCTIONS);
    }

    @BuildStep
    public void registerArc(CombinedIndexBuildItem combined,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = combined.getIndex();
        Set<ClassInfo> functionClasses = new HashSet<>();
        for (DotName ann : AzureFunctionsDotNames.PARAMETER_ANNOTATIONS) {
            Collection<AnnotationInstance> anns = index.getAnnotations(ann);
            anns.forEach(annotationInstance -> {
                ClassInfo ci = annotationInstance.target().asMethodParameter().method().declaringClass();
                //log.info("Param annotation: " + ci.name().toString());
                functionClasses
                        .add(ci);
            });
        }
        Collection<AnnotationInstance> anns = index.getAnnotations(AzureFunctionsDotNames.FUNCTION_NAME);
        anns.forEach(annotationInstance -> {
            ClassInfo ci = annotationInstance.target().asMethod().declaringClass();
            //log.info("FunctionName annotation: " + ci.name().toString());
            functionClasses.add(ci);
        });

        if (!functionClasses.isEmpty()) {
            AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                    .setDefaultScope(BuiltinScope.REQUEST.getName())
                    .setUnremovable();
            for (ClassInfo funcClass : functionClasses) {
                if (Modifier.isInterface(funcClass.flags()) || Modifier.isAbstract(funcClass.flags()))
                    continue;
                if (BuiltinScope.isDeclaredOn(funcClass)) {
                    //log.info("Add unremovable: " + funcClass.name().toString());
                    // It has a built-in scope - just mark it as unremovable
                    unremovableBeans
                            .produce(new UnremovableBeanBuildItem(
                                    new UnremovableBeanBuildItem.BeanClassNameExclusion(funcClass.name().toString())));
                } else {
                    // No built-in scope found - add as additional bean
                    //log.info("Add default: " + funcClass.name().toString());
                    builder.addBeanClass(funcClass.name().toString());
                }
            }
            additionalBeans.produce(builder.build());
        }
    }
}
