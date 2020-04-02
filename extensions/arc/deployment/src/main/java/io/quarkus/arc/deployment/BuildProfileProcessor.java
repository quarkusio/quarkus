package io.quarkus.arc.deployment;

import static io.quarkus.arc.processor.Annotations.find;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.configuration.ProfileManager;

public class BuildProfileProcessor {

    private static final DotName IF_BUILD_PROFILE = DotName.createSimple(IfBuildProfile.class.getName());
    private static final DotName UNLESS_BUILD_PROFILE = DotName.createSimple(UnlessBuildProfile.class.getName());

    /**
     * Uses {@link AnnotationsTransformer} to do the following:
     *
     * Adds the {@code @Alternative} annotation to any class or producer that is annotated with {code @IfBuildProfile}
     * when the specified profile doesn't match the current one.
     *
     * Adds the {@code @AlternativePriority} annotation to any class or producer that is annotated with
     * {code @IfBuildProfile} when the specified profile does match the current one.
     *
     * This effectively means that a bean annotated with {@code @IfBuildProfile} is only enabled when
     * the current profile matches that of the annotation
     */
    @BuildStep
    void ifBuildProfile(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD || kind == AnnotationTarget.Kind.CLASS
                        || kind == AnnotationTarget.Kind.FIELD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                AnnotationInstance enableOnBuildProfile = find(ctx.getAnnotations(), IF_BUILD_PROFILE);
                if (enableOnBuildProfile == null) {
                    return;
                }

                Transformation transform = ctx.transform();

                String profileOnInstance = enableOnBuildProfile.value().asString();
                if (profileOnInstance.equals(ProfileManager.getActiveProfile())) {
                    transform.add(DotNames.ALTERNATIVE_PRIORITY,
                            createAlternativePriority());
                } else {
                    transform.add(DotNames.ALTERNATIVE);
                }

                transform.done();
            }

        }));
    }

    /**
     * Uses {@link AnnotationsTransformer} to do the following:
     *
     * Adds the {@code @Alternative} annotation to any class or producer that is annotated with {code @UnlessBuildProfile}
     * when the specified profile does not match the current one.
     *
     * Adds the {@code @AlternativePriority} annotation to any class or producer that is annotated with
     * {code @UnlessBuildProfile} when the specified profile matches the current one.
     *
     * This effectively means that a bean annotated with {@code @UnlessBuildProfile} is only enabled when
     * the current profile dot not match that of the annotation
     */
    @BuildStep
    void unlessBuildProfile(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD || kind == AnnotationTarget.Kind.CLASS
                        || kind == AnnotationTarget.Kind.FIELD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                AnnotationInstance unlessBuildProfile = find(ctx.getAnnotations(), UNLESS_BUILD_PROFILE);
                if (unlessBuildProfile == null) {
                    return;
                }

                Transformation transform = ctx.transform();

                String profileOnInstance = unlessBuildProfile.value().asString();
                if (profileOnInstance.equals(ProfileManager.getActiveProfile())) {
                    transform.add(DotNames.ALTERNATIVE);
                } else {
                    transform.add(DotNames.ALTERNATIVE_PRIORITY,
                            createAlternativePriority());
                }

                transform.done();
            }

        }));
    }

    private AnnotationValue createAlternativePriority() {
        // use Integer.MAX_VALUE - 1 to avoid having conflicts with enabling beans via config
        return AnnotationValue.createIntegerValue("value", Integer.MAX_VALUE - 1);
    }
}
