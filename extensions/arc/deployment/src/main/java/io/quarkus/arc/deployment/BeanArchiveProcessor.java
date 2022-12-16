package io.quarkus.arc.deployment;

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

import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.DotNames;
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

    @BuildStep
    public BeanArchiveIndexBuildItem build(ArcConfig config, ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations,
            List<AdditionalBeanBuildItem> additionalBeans, List<GeneratedBeanBuildItem> generatedBeans,
            LiveReloadBuildItem liveReloadBuildItem, BuildProducer<GeneratedClassBuildItem> generatedClass,
            CustomScopeAnnotationsBuildItem customScopes, List<ExcludeDependencyBuildItem> excludeDependencyBuildItems,
            List<BeanArchivePredicateBuildItem> beanArchivePredicates)
            throws Exception {

        // First build an index from application archives
        IndexView applicationIndex = buildApplicationIndex(config, applicationArchivesBuildItem,
                additionalBeanDefiningAnnotations, customScopes, excludeDependencyBuildItems, beanArchivePredicates);

        // Then build additional index for beans added by extensions
        Indexer additionalBeanIndexer = new Indexer();
        List<String> additionalBeanClasses = new ArrayList<>();
        for (AdditionalBeanBuildItem i : additionalBeans) {
            additionalBeanClasses.addAll(i.getBeanClasses());
        }

        // Build the index for additional beans and generated bean classes
        Set<DotName> additionalIndex = new HashSet<>();
        Set<DotName> knownMissingClasses = new HashSet<>();
        for (String beanClass : additionalBeanClasses) {
            IndexingUtil.indexClass(beanClass, additionalBeanIndexer, applicationIndex, additionalIndex,
                    knownMissingClasses, Thread.currentThread().getContextClassLoader());
        }
        Set<DotName> generatedClassNames = new HashSet<>();
        for (GeneratedBeanBuildItem generatedBeanClass : generatedBeans) {
            IndexingUtil.indexClass(generatedBeanClass.getName(), additionalBeanIndexer, applicationIndex, additionalIndex,
                    knownMissingClasses, Thread.currentThread().getContextClassLoader(), generatedBeanClass.getData());
            generatedClassNames.add(DotName.createSimple(generatedBeanClass.getName().replace('/', '.')));
            generatedClass.produce(new GeneratedClassBuildItem(true, generatedBeanClass.getName(), generatedBeanClass.getData(),
                    generatedBeanClass.getSource()));
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
            List<BeanArchivePredicateBuildItem> beanArchivePredicates) {

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
        // Also include archives that are not bean archives but contain qualifiers or interceptor bindings
        beanDefiningAnnotations.add(DotNames.QUALIFIER);
        beanDefiningAnnotations.add(DotNames.INTERCEPTOR_BINDING);

        List<IndexView> indexes = new ArrayList<>();

        for (ApplicationArchive archive : applicationArchivesBuildItem.getApplicationArchives()) {
            if (isApplicationArchiveExcluded(config, excludeDependencyBuildItems, archive)) {
                continue;
            }
            IndexView index = archive.getIndex();
            if (isExplicitBeanArchive(archive) || isImplicitBeanArchive(index, beanDefiningAnnotations)
                    || isAdditionalBeanArchive(archive, beanArchivePredicates)) {
                indexes.add(index);
            }
        }
        indexes.add(applicationArchivesBuildItem.getRootArchive().getIndex());
        return CompositeIndex.create(indexes);
    }

    private boolean isExplicitBeanArchive(ApplicationArchive archive) {
        return archive.apply(tree -> tree.contains("META-INF/beans.xml") || tree.contains("WEB-INF/beans.xml"));
    }

    private boolean isImplicitBeanArchive(IndexView index, Set<DotName> beanDefiningAnnotations) {
        // NOTE: Implicit bean archive without beans.xml contains one or more bean classes with a bean defining annotation and no extension
        return index.getAllKnownImplementors(DotNames.EXTENSION).isEmpty()
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

    private boolean isApplicationArchiveExcluded(ArcConfig config, List<ExcludeDependencyBuildItem> excludeDependencyBuildItems,
            ApplicationArchive archive) {
        if (archive.getKey() != null) {
            final ArtifactKey key = archive.getKey();
            for (IndexDependencyConfig excludeDependency : config.excludeDependency.values()) {
                if (archiveMatches(key, excludeDependency.groupId, excludeDependency.artifactId,
                        excludeDependency.classifier)) {
                    return true;
                }
            }

            for (ExcludeDependencyBuildItem excludeDependencyBuildItem : excludeDependencyBuildItems) {
                if (archiveMatches(key, excludeDependencyBuildItem.getGroupId(), excludeDependencyBuildItem.getArtifactId(),
                        excludeDependencyBuildItem.getClassifier())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean archiveMatches(ArtifactKey key, String groupId, String artifactId, Optional<String> classifier) {

        if (Objects.equals(key.getArtifactId(), artifactId)
                && Objects.equals(key.getGroupId(), groupId)) {
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
