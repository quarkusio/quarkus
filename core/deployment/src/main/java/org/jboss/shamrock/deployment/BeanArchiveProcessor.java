package org.jboss.shamrock.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class BeanArchiveProcessor implements ResourceProcessor {

    @Inject
    private BeanArchiveIndex beanArchiveIndex;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {

        Set<ApplicationArchive> archives = archiveContext.getAllApplicationArchives();

        // The list is not exhaustive - it merely contains all annotations supported by Arc
        List<DotName> beanDefiningAnnotations = new ArrayList<>();
        beanDefiningAnnotations.add(DotName.createSimple("javax.enterprise.context.Dependent"));
        beanDefiningAnnotations.add(DotName.createSimple("javax.enterprise.context.RequestScoped"));
        beanDefiningAnnotations.add(DotName.createSimple("javax.enterprise.context.ApplicationScoped"));
        beanDefiningAnnotations.add(DotName.createSimple("javax.interceptor.Interceptor"));
        // TODO: we could also add @Inject and @Singleton although these are not officialy included

        // First find annotations annotated with "meta" bean defining annotations
        List<DotName> metaBeanDefiningAnnotations = Collections.singletonList(DotName.createSimple("javax.enterprise.inject.Stereotype"));
        for (ApplicationArchive archive : archives) {
            for (DotName metaAnnotation : metaBeanDefiningAnnotations) {
                Collection<AnnotationInstance> annotations = archive.getIndex().getAnnotations(metaAnnotation);
                if (!annotations.isEmpty()) {
                    for (AnnotationInstance annotationInstance : annotations) {
                        if (annotationInstance.target().kind() == Kind.CLASS) {
                            beanDefiningAnnotations.add(annotationInstance.target().asClass().name());
                        }
                    }
                }
            }
        }
        DotName extensionName = DotName.createSimple("javax.enterprise.inject.spi.Extension");

        List<IndexView> indexes = new ArrayList<>();

        for (ApplicationArchive archive : archives) {

            IndexView index = archive.getIndex();

            // TODO: this should not really be in core
            if (archive.getChildPath("META-INF/beans.xml") != null) {
                indexes.add(index);
            } else if (archive.getChildPath("WEB-INF/beans.xml") != null) {
                // TODO: how to handle WEB-INF?
                indexes.add(index);
            } else {
                // Implicit bean archive without beans.xml - contains one or more bean classes with a bean defining annotation and no extension
                if (index.getAllKnownImplementors(extensionName).isEmpty()) {
                    for (DotName beanDefiningAnnotation : beanDefiningAnnotations) {
                        if (!index.getAnnotations(beanDefiningAnnotation).isEmpty()) {
                            indexes.add(index);
                            break;
                        }
                    }
                }
            }
        }
        beanArchiveIndex.setIndex(CompositeIndex.create(indexes));
    }

    @Override
    public int getPriority() {
        // we want this to run early
        return -1000;
    }
}
