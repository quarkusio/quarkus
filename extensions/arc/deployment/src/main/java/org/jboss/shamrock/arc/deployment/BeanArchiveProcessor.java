/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.arc.deployment;

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
import org.jboss.protean.arc.processor.BeanDefiningAnnotation;
import org.jboss.protean.arc.processor.BeanDeployment;
import org.jboss.protean.arc.processor.DotNames;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.ApplicationArchive;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;

public class BeanArchiveProcessor {

    @Inject
    BuildProducer<BeanArchiveIndexBuildItem> beanArchiveIndexBuildProducer;

    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;

    @Inject
    List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations;

    @BuildStep
    public void build() throws Exception {

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

        Collection<DotName> beanDefiningAnnotations = BeanDeployment.initBeanDefiningAnnotations(additionalBeanDefiningAnnotations.stream()
                .map(bda -> new BeanDefiningAnnotation(bda.getName(), bda.getDefaultScope())).collect(Collectors.toList()), stereotypes);
        // Also include archives that are not bean archives but contain qualifiers or interceptor bindings
        beanDefiningAnnotations.add(DotNames.QUALIFIER);
        beanDefiningAnnotations.add(DotNames.INTERCEPTOR_BINDING);

        List<IndexView> indexes = new ArrayList<>();

        for (ApplicationArchive archive : applicationArchivesBuildItem.getApplicationArchives()) {
            IndexView index = archive.getIndex();
            // NOTE: Implicit bean archive without beans.xml contains one or more bean classes with a bean defining annotation and no extension
            if (archive.getChildPath("META-INF/beans.xml") != null || archive.getChildPath("WEB-INF/beans.xml") != null
                    || (index.getAllKnownImplementors(DotNames.EXTENSION).isEmpty() && containsBeanDefiningAnnotation(index, beanDefiningAnnotations))) {
                indexes.add(index);
            }
        }
        indexes.add(applicationArchivesBuildItem.getRootArchive().getIndex());
        beanArchiveIndexBuildProducer.produce(new BeanArchiveIndexBuildItem(CompositeIndex.create(indexes)));
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
