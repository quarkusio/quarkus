package io.quarkus.smallrye.openapi.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.OASFilter;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;

public class SmallRyeOpenApiFiltersProcessor {

    private static final Logger log = Logger.getLogger("io.quarkus.smallrye.openapi");

    private static final DotName NAME_OPEN_API_FILTER_ANNOTATION = DotName.createSimple(OpenApiFilter.class);
    private static final DotName NAME_OAS_FILTER = DotName.createSimple(OASFilter.class);

    @BuildStep
    void registerAnnotatedUserDefinedRuntimeFilters(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            DocumentFiltersBuildItem documentFiltersBuildItem) {

        Set<String> userDefinedRuntimeFilters = documentFiltersBuildItem
                .allFilterNamesFor(OpenApiFilter.RunStage.RUNTIME_STARTUP, OpenApiFilter.RunStage.RUNTIME_PER_REQUEST);

        String[] runtimeFilterClassNames = userDefinedRuntimeFilters.toArray(new String[] {});

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(runtimeFilterClassNames)
                .reason(getClass().getName()).build());

        // Make sure the filter beans are kept so they may be loaded programmatically at runtime
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(runtimeFilterClassNames));
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void validateOpenApiFilterStages(BeanArchiveIndexBuildItem indexBuildItem) {
        IndexView index = indexBuildItem.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(NAME_OPEN_API_FILTER_ANNOTATION);

        for (AnnotationInstance annotation : annotations) {
            AnnotationValue stagesValue = annotation.valueWithDefault(index, "stages");
            if (stagesValue.asArrayList().isEmpty()) {
                log.warnf(
                        "@OpenApiFilter on '%s' will not be run, since the stages array is set to an empty array (stages = {}).",
                        annotation.target().asClass().name());
            }
        }
    }

    private List<String> extractDocumentNames(IndexView index, AnnotationInstance openApiFilterAnnotation) {

        AnnotationValue annotationValue = openApiFilterAnnotation.valueWithDefault(index, "documentNames");

        List<String> documentNames = new ArrayList<>();
        for (AnnotationValue value : annotationValue.asArrayList()) {
            documentNames.add(value.asString());
        }

        return documentNames;
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void validateOpenApiFilterDocumentNames(SmallRyeOpenApiConfig config,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem) {
        IndexView index = openApiFilteredIndexViewBuildItem.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(NAME_OPEN_API_FILTER_ANNOTATION);

        Map<DotName, Set<String>> problematicDocumentNames = new HashMap<>();
        for (AnnotationInstance annotation : annotations) {
            List<String> documentNames = extractDocumentNames(index, annotation);

            for (String documentName : documentNames) {
                if (documentName.equals(OpenApiFilter.DEFAULT_DOCUMENT_NAME)) {
                    continue;
                }
                if (documentName.equals(OpenApiFilter.FILTER_RUN_FOR_ANY_DOCUMENT)) {
                    continue;
                }

                if (config.documents().containsKey(documentName)) {
                    continue;
                }

                problematicDocumentNames.computeIfAbsent(annotation.target().asClass().name(), ignored -> new LinkedHashSet<>())
                        .add(documentName);
            }
        }

        if (!problematicDocumentNames.isEmpty()) {
            Set<String> validDocumentNamesValues = new HashSet<>(config.documents().keySet());
            validDocumentNamesValues.add(OpenApiFilter.FILTER_RUN_FOR_ANY_DOCUMENT);

            String message = """
                    Following instances of the OpenApiFilter annotation are invalid because of a misconfigured documentNames value.
                    Valid values are: %s
                    """
                    .formatted(validDocumentNamesValues);
            message += problematicDocumentNames.entrySet().stream()
                    .map(entry -> String.format("@OpenApiFilter '%s' references unknown document names: %s",
                            entry.getKey(),
                            entry.getValue()))
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(message);
        }
    }

    @BuildStep
    DocumentFiltersBuildItem produceFilters(SmallRyeOpenApiConfig smallRyeOpenApiConfig,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem) {
        Set<String> allDocumentNames = smallRyeOpenApiConfig.documents().keySet();
        IndexView index = openApiFilteredIndexViewBuildItem.getIndex();

        Comparator<AnnotationInstance> comparator = Comparator
                .<AnnotationInstance, Integer> comparing(x -> x.valueWithDefault(index, "priority").asInt())
                .reversed();

        Set<AnnotationInstance> allFilterInstances = index
                .getAnnotations(NAME_OPEN_API_FILTER_ANNOTATION)
                .stream()
                .filter(ai -> ai.target().asClass().interfaceNames().contains(NAME_OAS_FILTER))
                .sorted(comparator)
                // LinkedHashSet, since the index contains duplicates
                // see https://github.com/smallrye/smallrye-open-api/issues/2191
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Config config = ConfigProvider.getConfig();
        DotName configDefinedFilterName = config.getOptionalValue(OASConfig.FILTER, String.class).map(DotName::createSimple)
                .orElse(null);

        DocumentFiltersBuildItem.Builder builder = DocumentFiltersBuildItem.builder();
        builder.addDocuments(smallRyeOpenApiConfig);

        boolean configProvidedFilterAlreadyRegistered = false;
        for (AnnotationInstance filterInstance : allFilterInstances) {
            Collection<String> filterDocumentNames = extractDocumentNames(index, filterInstance);
            if (filterDocumentNames.contains(OpenApiFilter.FILTER_RUN_FOR_ANY_DOCUMENT)) {
                filterDocumentNames = allDocumentNames;
            }

            Set<OpenApiFilter.RunStage> filterStages = parseStages(filterInstance, index);

            ClassInfo classTarget = filterInstance.target().asClass();
            String className = classTarget.name().toString();

            for (String filterDocumentName : filterDocumentNames) {
                for (OpenApiFilter.RunStage filterStage : filterStages) {
                    builder.addFilterName(filterDocumentName, filterStage, className);
                }
            }

            if (classTarget.name().equals(configDefinedFilterName)) {
                configProvidedFilterAlreadyRegistered = true;
            }
        }

        if (!configProvidedFilterAlreadyRegistered && configDefinedFilterName != null) {
            for (String filterDocumentName : allDocumentNames) {
                builder.addFilterName(filterDocumentName, OpenApiFilter.RunStage.RUNTIME_STARTUP,
                        configDefinedFilterName.toString());
            }
        }

        return builder.build();
    }

    /**
     * parses the effective stages from {@link OpenApiFilter#stages()}.
     *
     * @param ai the OpenApiFilter annotation placed on an OASFilter implementation
     * @param index
     * @return set of the {@link OpenApiFilter.RunStage}s this OasFilter should run in, never null.
     */
    private Set<OpenApiFilter.RunStage> parseStages(AnnotationInstance ai, IndexView index) {

        Set<OpenApiFilter.RunStage> runStages = EnumSet.noneOf(OpenApiFilter.RunStage.class);
        AnnotationValue stages = ai.valueWithDefault(index, "stages");
        if (stages != null) {
            for (AnnotationValue sv : stages.asArrayList()) {
                runStages.add(OpenApiFilter.RunStage.valueOf(sv.asEnum()));
            }
        }

        return runStages;
    }
}
