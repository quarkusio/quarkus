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

package io.quarkus.hibernate.orm.panache.deployment;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.persistence.EntityManager;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public final class PanacheResourceProcessor {

    private static final DotName DOTNAME_PANACHE_REPOSITORY_BASE = DotName.createSimple(PanacheRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_REPOSITORY = DotName.createSimple(PanacheRepository.class.getName());
    private static final DotName DOTNAME_PANACHE_ENTITY_BASE = DotName.createSimple(PanacheEntityBase.class.getName());
    private static final DotName DOTNAME_PANACHE_ENTITY = DotName.createSimple(PanacheEntity.class.getName());

    private static final Set<DotName> UNREMOVABLE_BEANS = Collections.singleton(
            DotName.createSimple(EntityManager.class.getName()));

    @BuildStep
    List<AdditionalJpaModelBuildItem> produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return Collections.singletonList(
                new AdditionalJpaModelBuildItem(PanacheEntity.class));
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo beanInfo) {
                for (Type t : beanInfo.getTypes()) {
                    if (UNREMOVABLE_BEANS.contains(t.name())) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    @BuildStep
    void build(CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            HibernateEnhancersRegisteredBuildItem hibernateMarker) throws Exception {

        PanacheJpaRepositoryEnhancer daoEnhancer = new PanacheJpaRepositoryEnhancer();
        Set<String> daoClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY_BASE)) {
            // Skip PanacheRepository
            if (classInfo.name().equals(DOTNAME_PANACHE_REPOSITORY))
                continue;
            daoClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY)) {
            daoClasses.add(classInfo.name().toString());
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, daoEnhancer));
        }

        PanacheJpaEntityEnhancer modelEnhancer = new PanacheJpaEntityEnhancer();
        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheEntity if we ask for subtypes of PanacheEntityBase
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY_BASE)) {
            // FIXME: should we really skip PanacheEntity or all MappedSuperClass?
            if (classInfo.name().equals(DOTNAME_PANACHE_ENTITY))
                continue;
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY)) {
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, modelEnhancer));
        }

        if (!modelEnhancer.entities.isEmpty()) {
            PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(modelEnhancer.entities);
            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!modelClasses.contains(className)) {
                    transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }
    }

}
