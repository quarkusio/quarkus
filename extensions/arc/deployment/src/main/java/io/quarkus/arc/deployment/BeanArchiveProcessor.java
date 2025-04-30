package io.quarkus.arc.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.AdditionalBean;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ExcludeDependencyBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.index.IndexDependencyConfig;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.index.PersistentClassIndex;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

public class BeanArchiveProcessor {

    private static final Logger LOGGER = Logger.getLogger(BeanArchiveProcessor.class);

    @BuildStep
    public BeanArchiveIndexBuildItem build(ArcConfig config, ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations,
            List<AdditionalBeanBuildItem> additionalBeans, List<GeneratedBeanBuildItem> generatedBeans,
            LiveReloadBuildItem liveReloadBuildItem, BuildProducer<GeneratedClassBuildItem> generatedClass,
            CustomScopeAnnotationsBuildItem customScopes, List<ExcludeDependencyBuildItem> excludeDependencyBuildItems,
            List<BeanArchivePredicateBuildItem> beanArchivePredicates,
            List<KnownCompatibleBeanArchiveBuildItem> knownCompatibleBeanArchives,
            BuildCompatibleExtensionsBuildItem buildCompatibleExtensions,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformations)
            throws Exception {

        // First build an index from application archives
        IndexView applicationIndex = buildApplicationIndex(config, applicationArchivesBuildItem,
                additionalBeanDefiningAnnotations, customScopes, excludeDependencyBuildItems, beanArchivePredicates,
                new KnownCompatibleBeanArchives(knownCompatibleBeanArchives));

        // Then build additional index for beans added by extensions
        Indexer additionalBeanIndexer = new Indexer();
        List<String> additionalBeanClasses = new ArrayList<>();
        for (AdditionalBeanBuildItem i : additionalBeans) {
            additionalBeanClasses.addAll(i.getBeanClasses());
        }

        Set<String> additionalBeansFromExtensions = new HashSet<>();
        buildCompatibleExtensions.entrypoint.runDiscovery(applicationIndex, additionalBeansFromExtensions);
        additionalBeanClasses.addAll(additionalBeansFromExtensions);
        annotationsTransformations.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (additionalBeansFromExtensions.contains(ctx.getTarget().asClass().name().toString())) {
                    // make all the `@Discovery`-registered classes beans
                    ctx.transform().add(AdditionalBean.class).done();
                }
            }
        }));

        // Build the index for additional beans and generated bean classes
        Set<DotName> additionalIndex = new HashSet<>();
        Set<DotName> knownMissingClasses = new HashSet<>();
        for (String beanClass : additionalBeanClasses) {
            IndexingUtil.indexClass(beanClass, additionalBeanIndexer, applicationIndex, additionalIndex,
                    knownMissingClasses, Thread.currentThread().getContextClassLoader());
        }
        Set<DotName> generatedClassNames = new HashSet<>();
        for (GeneratedBeanBuildItem generatedBean : generatedBeans) {
            IndexingUtil.indexClass(generatedBean.getName(), additionalBeanIndexer, applicationIndex, additionalIndex,
                    knownMissingClasses, Thread.currentThread().getContextClassLoader(), generatedBean.getData());
            generatedClassNames.add(DotName.createSimple(generatedBean.getName().replace('/', '.')));
            generatedClass.produce(new GeneratedClassBuildItem(generatedBean.isApplicationClass(), generatedBean.getName(),
                    generatedBean.getData(),
                    generatedBean.getSource()));
        }

        PersistentClassIndex index = liveReloadBuildItem.getContextObject(PersistentClassIndex.class);
        if (index == null) {
            index = new PersistentClassIndex();
            liveReloadBuildItem.setContextObject(PersistentClassIndex.class, index);
        }

        Map<DotName, Optional<ClassInfo>> additionalClasses = index.getAdditionalClasses();
        for (DotName knownMissingClass : knownMissingClasses) {
            additionalClasses.put(knownMissingClass, Optional.empty());
        }

        IndexView immutableBeanArchiveIndex = BeanArchives.buildImmutableBeanArchiveIndex(applicationIndex,
                additionalBeanIndexer.complete());
        IndexView computingBeanArchiveIndex = BeanArchives.buildComputingBeanArchiveIndex(
                Thread.currentThread().getContextClassLoader(),
                additionalClasses, immutableBeanArchiveIndex);
        return new BeanArchiveIndexBuildItem(computingBeanArchiveIndex, immutableBeanArchiveIndex, generatedClassNames);
    }

    private IndexView buildApplicationIndex(ArcConfig config, ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations,
            CustomScopeAnnotationsBuildItem customScopes, List<ExcludeDependencyBuildItem> excludeDependencyBuildItems,
            List<BeanArchivePredicateBuildItem> beanArchivePredicates,
            KnownCompatibleBeanArchives knownCompatibleBeanArchives) {

        Set<ApplicationArchive> archives = applicationArchivesBuildItem.getAllApplicationArchives();

        // We need to collect all stereotype annotations first
        Set<DotName> stereotypes = new HashSet<>();
        for (ApplicationArchive archive : archives) {
            Collection<AnnotationInstance> annotations = archive.getIndex().getAnnotations(DotNames.STEREOTYPE);
            if (!annotations.isEmpty()) {
                for (AnnotationInstance annotationInstance : annotations) {
                    if (annotationInstance.target().kind() == Kind.CLASS) {
                        stereotypes.add(annotationInstance.target().asClass().name());
                    }
                }
            }
        }

        Set<DotName> beanDefiningAnnotations = BeanDeployment
                .initBeanDefiningAnnotations(additionalBeanDefiningAnnotations.stream()
                        .map(bda -> new BeanDefiningAnnotation(bda.getName(), bda.getDefaultScope()))
                        .collect(Collectors.toList()), stereotypes);
        beanDefiningAnnotations.addAll(customScopes.getCustomScopeNames());
        // Also include archives that are not bean archives but contain scopes, qualifiers,
        // interceptor bindings, interceptors or decorators
        beanDefiningAnnotations.add(DotNames.SCOPE);
        beanDefiningAnnotations.add(DotNames.NORMAL_SCOPE);
        beanDefiningAnnotations.add(DotNames.QUALIFIER);
        beanDefiningAnnotations.add(DotNames.INTERCEPTOR_BINDING);
        beanDefiningAnnotations.add(DotNames.DECORATOR);
        beanDefiningAnnotations.add(DotNames.INTERCEPTOR);

        boolean rootIsAlwaysBeanArchive = !config.strictCompatibility();
        Collection<ApplicationArchive> candidateArchives = applicationArchivesBuildItem.getApplicationArchives();
        if (!rootIsAlwaysBeanArchive) {
            candidateArchives = new ArrayList<>(candidateArchives);
            candidateArchives.add(applicationArchivesBuildItem.getRootArchive());
        }

        List<IndexView> indexes = new ArrayList<>();

        for (ApplicationArchive archive : candidateArchives) {
            if (isApplicationArchiveExcluded(config, excludeDependencyBuildItems, archive)) {
                continue;
            }
            if (!possiblyBeanArchive(archive, knownCompatibleBeanArchives)) {
                continue;
            }
            IndexView index = archive.getIndex();
            if (isExplicitBeanArchive(archive)
                    || isImplicitBeanArchive(index, beanDefiningAnnotations)
                    || isAdditionalBeanArchive(archive, beanArchivePredicates)) {
                indexes.add(index);
            }
        }
        if (rootIsAlwaysBeanArchive) {
            indexes.add(applicationArchivesBuildItem.getRootArchive().getIndex());
        }
        return CompositeIndex.create(indexes);
    }

    private boolean isExplicitBeanArchive(ApplicationArchive archive) {
        return archive.apply(tree -> tree.contains("META-INF/beans.xml") || tree.contains("WEB-INF/beans.xml"));
    }

    private boolean isImplicitBeanArchive(IndexView index, Set<DotName> beanDefiningAnnotations) {
        // NOTE: Implicit bean archive without beans.xml contains one or more bean classes with a bean defining annotation and no extension
        return index.getAllKnownImplementors(DotNames.EXTENSION).isEmpty()
                && index.getAllKnownImplementors(DotNames.BUILD_COMPATIBLE_EXTENSION).isEmpty()
                && containsBeanDefiningAnnotation(index, beanDefiningAnnotations);
    }

    private boolean isAdditionalBeanArchive(ApplicationArchive archive,
            List<BeanArchivePredicateBuildItem> beanArchivePredicates) {
        for (BeanArchivePredicateBuildItem p : beanArchivePredicates) {
            if (p.getPredicate().test(archive)) {
                return true;
            }
        }
        return false;
    }

    private boolean possiblyBeanArchive(ApplicationArchive archive,
            KnownCompatibleBeanArchives knownCompatibleBeanArchives) {
        return archive.apply(tree -> {
            boolean result = true;
            for (String beansXml : List.of("META-INF/beans.xml", "WEB-INF/beans.xml")) {
                result &= tree.apply(beansXml, pathVisit -> {
                    if (pathVisit == null) {
                        return true;
                    }

                    // crude but enough
                    try {
                        String text = Files.readString(pathVisit.getPath());
                        if (text.contains("bean-discovery-mode='none'")
                                || text.contains("bean-discovery-mode=\"none\"")) {
                            return false;
                        }

                        if (text.contains("bean-discovery-mode='all'")
                                || text.contains("bean-discovery-mode=\"all\"")) {

                            if (!knownCompatibleBeanArchives.isKnownCompatible(archive)) {
                                LOGGER.warnf("Detected bean archive with bean discovery mode of 'all', "
                                        + "this is not portable in CDI Lite and is treated as 'annotated' in Quarkus! "
                                        + "Path to beans.xml: %s",
                                        archive.getResolvedDependency() != null
                                                ? archive.getResolvedDependency().toCompactCoords() + ":" + pathVisit.getPath()
                                                : pathVisit.getPath());
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    return true;
                });
            }
            return result;
        });
    }

    private boolean isApplicationArchiveExcluded(ArcConfig config, List<ExcludeDependencyBuildItem> excludeDependencyBuildItems,
            ApplicationArchive archive) {
        if (archive.getKey() != null) {
            final ArtifactKey key = archive.getKey();
            for (IndexDependencyConfig excludeDependency : config.excludeDependency().values()) {
                if (archiveMatches(key, excludeDependency.groupId(), excludeDependency.artifactId(),
                        excludeDependency.classifier())) {
                    return true;
                }
            }

            for (ExcludeDependencyBuildItem excludeDependencyBuildItem : excludeDependencyBuildItems) {
                if (archiveMatches(key, excludeDependencyBuildItem.getGroupId(),
                        Optional.ofNullable(excludeDependencyBuildItem.getArtifactId()),
                        excludeDependencyBuildItem.getClassifier())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean archiveMatches(ArtifactKey key, String groupId, Optional<String> artifactId,
            Optional<String> classifier) {
        if (Objects.equals(key.getGroupId(), groupId)
                && (artifactId.isEmpty() || Objects.equals(key.getArtifactId(), artifactId.get()))) {
            if (classifier.isPresent() && Objects.equals(key.getClassifier(), classifier.get())) {
                return true;
            } else if (!classifier.isPresent() && ArtifactCoords.DEFAULT_CLASSIFIER.equals(key.getClassifier())) {
                return true;
            }
        }
        return false;
    }

    boolean containsBeanDefiningAnnotation(IndexView index, Collection<DotName> beanDefiningAnnotations) {
        for (DotName beanDefiningAnnotation : beanDefiningAnnotations) {
            if (!index.getAnnotations(beanDefiningAnnotation).isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
