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

package org.jboss.shamrock.panache;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.persistence.EntityManager;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.panache.jpa.DaoBase;
import org.jboss.panache.jpa.EntityBase;
import org.jboss.panache.jpa.Model;
import org.jboss.protean.arc.processor.BeanInfo;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.shamrock.arc.deployment.UnremovableBeanBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.ApplicationIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.jpa.AdditionalJpaModelBuildItem;
import org.jboss.shamrock.jpa.HibernateEnhancersRegisteredBuildItem;

/**
 */
public final class PanacheResourceProcessor {


    private static final DotName DOTNAME_DAO_BASE = DotName.createSimple(DaoBase.class.getName());
    private static final DotName DOTNAME_ENTITY_BASE = DotName.createSimple(EntityBase.class.getName());
    private static final DotName DOTNAME_MODEL = DotName.createSimple(Model.class.getName());

    private static final Set<DotName> UNREMOVABLE_BEANS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    DotName.createSimple(EntityManager.class.getName())

            )));

    @BuildStep
    List<AdditionalJpaModelBuildItem> produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return Arrays.asList(
                new AdditionalJpaModelBuildItem(Model.class));
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailible() {
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo beanInfo) {
                for(Type t : beanInfo.getTypes()) {
                    if(UNREMOVABLE_BEANS.contains(t.name())) {
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

        PanacheJpaDaoEnhancer daoEnhancer = new PanacheJpaDaoEnhancer();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_DAO_BASE)) {
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), daoEnhancer));
        }

        PanacheJpaModelEnhancer modelEnhancer = new PanacheJpaModelEnhancer();
        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of Model if we ask for subtypes of EntityBase
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_ENTITY_BASE)) {
            // skip Model
            if(classInfo.name().equals(DOTNAME_MODEL))
                continue;
            modelClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_MODEL)) {
            modelEnhancer.collectFields(classInfo);
            modelClasses.add(classInfo.name().toString());
        }
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, modelEnhancer));
        }
        
        PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(modelEnhancer.entities);
        for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
            String className = classInfo.name().toString();
            if(!modelClasses.contains(className)) {
                transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
            }
        }
    }


    static final class ProcessorClassOutput implements ClassOutput {
        private final BuildProducer<GeneratedClassBuildItem> producer;

        ProcessorClassOutput(BuildProducer<GeneratedClassBuildItem> producer) {
            this.producer = producer;
        }

        public void write(final String name, final byte[] data) {
            producer.produce(new GeneratedClassBuildItem(false, name, data));
        }

    }}
