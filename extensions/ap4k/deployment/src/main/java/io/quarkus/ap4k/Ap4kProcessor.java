package io.quarkus.ap4k;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import io.ap4k.Generator;
import io.ap4k.Session;
import io.ap4k.SessionWriter;
import io.ap4k.processor.SimpleFileWriter;
import io.ap4k.project.Project;
import io.quarkus.deployment.BuildInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BuildInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;

class Ap4kProcessor {

    @Inject
    BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer;

    @Inject
    BuildProducer<FeatureBuildItem> featureProducer;

    @BuildStep
    public void build(ApplicationIndexBuildItem applicationIndexBuildItem, BuildInfoBuildItem buildInfoBuildItem) {

        // the resources that ap4k's execution will result in, will later on be written
        // by quarkus in the wiring-classes directory
        // the location is needed in order to properly support s2i build triggering
        final Path rootPath = Paths.get(buildInfoBuildItem.getBuildInfo().getWiringClassesDir());
        if (rootPath.toString().isEmpty()) { //this is done to ap4k from failing when the necessary input has not been provided (during tests for example)
            return;
        }
        final Path ap4kRoot = rootPath.resolve("META-INF/ap4k");
        final BuildInfo buildInfo = buildInfoBuildItem.getBuildInfo();

        // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
        final SessionWriter sessionWriter = new SimpleFileWriter(ap4kRoot, false);
        final String packaging = "jar";
        sessionWriter.setProject(new Project(Paths.get(buildInfo.getBaseDir()),
                new io.ap4k.project.BuildInfo(
                        buildInfo.getName(), buildInfo.getVersion(), packaging,
                        rootPath.getParent().resolve(buildInfo.getFinalName() + "-runner." + packaging), rootPath)));
        final Session session = Session.getSession();
        session.setWriter(sessionWriter);

        final Index index = applicationIndexBuildItem.getIndex();
        final ServiceLoader<Generator> serviceLoader = ServiceLoader.load(Generator.class);
        for (Generator generator : serviceLoader) {

            final Map<String, Object> generatorInput = new HashMap<>();

            for (Class supportedAnnotation : generator.getSupportedAnnotations()) {
                final String supportedAnnotationName = supportedAnnotation.getName();
                final List<AnnotationInstance> instances = index.getAnnotations(DotName.createSimple(supportedAnnotationName));
                if (instances.isEmpty()) {
                    continue;
                }

                failIfMoreThanOneInstanceExists(supportedAnnotation, instances);

                final AnnotationInstance instance = instances.get(0);
                final Map<String, Object> instanceValues = instanceToMap(instance);
                generatorInput.put(supportedAnnotationName, instanceValues);
            }

            if (!generatorInput.isEmpty()) {
                generator.add(generatorInput);
            }
        }

        final Map<String, String> generatedResourcesMap = session.close();
        for (String generatedResourceFullPath : generatedResourcesMap.keySet()) {
            generatedResourceProducer.produce(
                    new GeneratedResourceBuildItem(
                            // we need to make sure we are only passing the relative path to the build item
                            generatedResourceFullPath.replace(rootPath.toString() + "/", ""),
                            generatedResourcesMap.get(generatedResourceFullPath).getBytes()));
        }

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.AP4K));
    }

    private void failIfMoreThanOneInstanceExists(Class supportedAnnotation, List<AnnotationInstance> instances) {
        if (instances.size() <= 1)
            return;
        instances.size();

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (AnnotationInstance annotationInstance : instances) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(annotationInstance.target().asClass().name().toString());
        }
        throw new RuntimeException("Multiple classes ( " + sb.toString() + ") have been annotated with " + supportedAnnotation
                + " which is currently not supported");
    }

    private Map<String, Object> instanceToMap(AnnotationInstance instance) {
        final Map<String, Object> instanceValues = new HashMap<>();
        for (AnnotationValue value : instance.values()) {
            if (value.kind() == AnnotationValue.Kind.ARRAY) {
                List<Object> nested = new ArrayList<>();
                if (value.componentKind() == AnnotationValue.Kind.NESTED) {
                    final AnnotationInstance[] nestedInstances = value.asNestedArray();
                    for (AnnotationInstance nestedInstance : nestedInstances) {
                        nested.add(instanceToMap(nestedInstance));
                    }
                } else if (value.componentKind() == AnnotationValue.Kind.STRING) {
                    final String[] values = value.asStringArray();
                    for (String val : values) {
                        nested.add(val);
                    }
                } else if (value.componentKind() == AnnotationValue.Kind.INTEGER) {
                    final int[] values = value.asIntArray();
                    for (int val : values) {
                        nested.add(val);
                    }
                } else if (value.componentKind() == AnnotationValue.Kind.LONG) {
                    final long[] values = value.asLongArray();
                    for (long val : values) {
                        nested.add(val);
                    }
                } else if (value.componentKind() == AnnotationValue.Kind.BOOLEAN) {
                    final boolean[] values = value.asBooleanArray();
                    for (boolean val : values) {
                        nested.add(val);
                    }
                } else if (value.componentKind() == AnnotationValue.Kind.ENUM) {
                    final String[] values = value.asEnumArray();
                    for (String val : values) {
                        nested.add(val);
                    }
                }
                if (!nested.isEmpty()) {
                    instanceValues.put(value.name(), toArray(nested));
                }
            } else if (value.kind() == AnnotationValue.Kind.NESTED) {
                instanceValues.put(value.name(), instanceToMap(value.asNested()));
            } else {
                instanceValues.put(value.name(), value.value());
            }
        }
        return instanceValues;
    }

    private <T> T[] toArray(List<T> list) {
        Class clazz = list.get(0).getClass();
        T[] array = (T[]) java.lang.reflect.Array.newInstance(clazz, list.size());
        return list.toArray(array);
    }
}
