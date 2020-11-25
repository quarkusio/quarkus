package io.quarkus.arc.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import com.google.common.base.Objects;

import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.LifecycleEventRunner;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.index.IndexDependencyConfig;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.index.PersistentClassIndex;

public class BeanArchiveProcessor {

    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;

    @Inject
    List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations;

    @Inject
    List<AdditionalBeanBuildItem> additionalBeans;

    @Inject
    List<GeneratedBeanBuildItem> generatedBeans;

    ArcConfig config;

    @BuildStep
    public BeanArchiveIndexBuildItem build(LiveReloadBuildItem liveReloadBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass) throws Exception {

        // First build an index from application archives
        IndexView applicationIndex = buildApplicationIndex();

        // Then build additional index for beans added by extensions
        Indexer additionalBeanIndexer = new Indexer();
        List<String> additionalBeans = new ArrayList<>();
        for (AdditionalBeanBuildItem i : this.additionalBeans) {
            additionalBeans.addAll(i.getBeanClasses());
        }
        // NOTE: the types added directly must always declare a scope annotation otherwise they will be ignored during bean discovery
        additionalBeans.add(LifecycleEventRunner.class.getName());

        // Build the index for additional beans and generated bean classes
        Set<DotName> additionalIndex = new HashSet<>();
        for (String beanClass : additionalBeans) {
            IndexingUtil.indexClass(beanClass, additionalBeanIndexer, applicationIndex, additionalIndex,
                    Thread.currentThread().getContextClassLoader());
        }
        Set<DotName> generatedClassNames = new HashSet<>();
        for (GeneratedBeanBuildItem generatedBeanClass : generatedBeans) {
            IndexingUtil.indexClass(generatedBeanClass.getName(), additionalBeanIndexer, applicationIndex, additionalIndex,
                    Thread.currentThread().getContextClassLoader(),
                    generatedBeanClass.getData());
            generatedClassNames.add(DotName.createSimple(generatedBeanClass.getName().replace('/', '.')));
            generatedClass.produce(new GeneratedClassBuildItem(true, generatedBeanClass.getName(), generatedBeanClass.getData(),
                    generatedBeanClass.getSource()));
        }

        PersistentClassIndex index = liveReloadBuildItem.getContextObject(PersistentClassIndex.class);
        if (index == null) {
            index = new PersistentClassIndex();
            liveReloadBuildItem.setContextObject(PersistentClassIndex.class, index);
        }

        // Finally, index ArC/CDI API built-in classes
        return new BeanArchiveIndexBuildItem(
                BeanArchives.buildBeanArchiveIndex(Thread.currentThread().getContextClassLoader(), index.getAdditionalClasses(),
                        applicationIndex,
                        additionalBeanIndexer.complete()),
                generatedClassNames,
                additionalBeans);
    }

    private IndexView buildApplicationIndex() {

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

        Collection<DotName> beanDefiningAnnotations = BeanDeployment
                .initBeanDefiningAnnotations(additionalBeanDefiningAnnotations.stream()
                        .map(bda -> new BeanDefiningAnnotation(bda.getName(), bda.getDefaultScope()))
                        .collect(Collectors.toList()), stereotypes);
        // Also include archives that are not bean archives but contain qualifiers or interceptor bindings
        beanDefiningAnnotations.add(DotNames.QUALIFIER);
        beanDefiningAnnotations.add(DotNames.INTERCEPTOR_BINDING);

        List<IndexView> indexes = new ArrayList<>();

        for (ApplicationArchive archive : applicationArchivesBuildItem.getApplicationArchives()) {
            if (isApplicationArchiveExcluded(archive)) {
                continue;
            }
            IndexView index = archive.getIndex();
            // NOTE: Implicit bean archive without beans.xml contains one or more bean classes with a bean defining annotation and no extension
            if (archive.getChildPath("META-INF/beans.xml") != null || archive.getChildPath("WEB-INF/beans.xml") != null
                    || (index.getAllKnownImplementors(DotNames.EXTENSION).isEmpty()
                            && containsBeanDefiningAnnotation(index, beanDefiningAnnotations))) {
                indexes.add(index);
            }
        }
        indexes.add(applicationArchivesBuildItem.getRootArchive().getIndex());
        return CompositeIndex.create(indexes);
    }

    private boolean isApplicationArchiveExcluded(ApplicationArchive archive) {
        if (archive.getArtifactKey() != null) {
            AppArtifactKey key = archive.getArtifactKey();
            for (IndexDependencyConfig excludeDependency : config.excludeDependency.values()) {
                if (Objects.equal(key.getArtifactId(), excludeDependency.artifactId)
                        && Objects.equal(key.getGroupId(), excludeDependency.groupId)) {
                    if (excludeDependency.classifier.isPresent()) {
                        return Objects.equal(key.getClassifier(), excludeDependency.classifier.get());
                    } else {
                        return true;
                    }
                }
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
