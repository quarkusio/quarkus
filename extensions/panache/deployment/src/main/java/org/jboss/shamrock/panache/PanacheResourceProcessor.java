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

import java.net.URL;
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
import org.jboss.panache.jpa.Controller;
import org.jboss.panache.jpa.DaoBase;
import org.jboss.panache.jpa.EntityBase;
import org.jboss.panache.jpa.Model;
import org.jboss.panache.rx.PgPoolProducer;
import org.jboss.panache.rx.RxEntityBase;
import org.jboss.panache.rx.RxModel;
import org.jboss.protean.arc.processor.BeanInfo;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.UnremovableBeanBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.jpa.AdditionalJpaModelBuildItem;

import io.reactiverse.reactivex.pgclient.PgPool;

/**
 */
public final class PanacheResourceProcessor {


    private static final DotName DOTNAME_CONTROLLER_BASE = DotName.createSimple(Controller.class.getName());
    private static final DotName DOTNAME_DAO_BASE = DotName.createSimple(DaoBase.class.getName());
    private static final DotName DOTNAME_ENTITY_BASE = DotName.createSimple(EntityBase.class.getName());
    private static final DotName DOTNAME_RX_ENTITY_BASE = DotName.createSimple(RxEntityBase.class.getName());
    private static final DotName DOTNAME_MODEL = DotName.createSimple(Model.class.getName());
    private static final DotName DOTNAME_RX_MODEL = DotName.createSimple(RxModel.class.getName());

    private static final Set<DotName> UNREMOVABLE_BEANS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    DotName.createSimple(EntityManager.class.getName()),
                    DotName.createSimple(PgPool.class.getName())

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
    AdditionalBeanBuildItem producePgPool() {
        return new AdditionalBeanBuildItem(PgPoolProducer.class);
    }
    
    @BuildStep
    void build(CombinedIndexBuildItem index,
               BuildProducer<BytecodeTransformerBuildItem> transformers,
               BuildProducer<GeneratedClassBuildItem> generatedClasses,
               BuildProducer<SubstrateResourceBuildItem> resources) throws Exception {

        // FIXME: harmonize with ORM
        URL load = Thread.currentThread().getContextClassLoader().getResource("META-INF/load.sql");
        if(load != null)
            resources.produce(new SubstrateResourceBuildItem("META-INF/load.sql"));
        
        PanacheRouterEnhancer routerEnhancer = new PanacheRouterEnhancer();
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_CONTROLLER_BASE)) {
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), routerEnhancer));
        }

        PanacheJpaDaoEnhancer daoEnhancer = new PanacheJpaDaoEnhancer();
        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(DOTNAME_DAO_BASE)) {
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), daoEnhancer));
        }

        PanacheJpaModelEnhancer modelEnhancer = new PanacheJpaModelEnhancer();
        Set<String> modelClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_ENTITY_BASE)) {
            // skip Model
            if(classInfo.name().equals(DOTNAME_MODEL))
                continue;
            modelClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_MODEL)) {
            modelClasses.add(classInfo.name().toString());
        }
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, modelEnhancer));
        }
        
        PanacheRxModelEnhancer rxModelEnhancer = new PanacheRxModelEnhancer();
        Set<String> rxModelClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_RX_ENTITY_BASE)) {
            // skip RxModel
            if(classInfo.name().equals(DOTNAME_RX_MODEL))
                continue;
            rxModelClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_RX_MODEL)) {
            rxModelClasses.add(classInfo.name().toString());
        }
        for (String rxModelClass : rxModelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(rxModelClass, rxModelEnhancer));
            PanacheRxModelInfoGenerator.generateModelClass(rxModelClass, generatedClasses);
        }

        // this just deadlocks, probably fighting with JPA
//        System.out.println("EXPORT C");
//        try {
//            final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
//            System.out.println("EXPORT1aa");
//            final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );
//            System.out.println("EXPORT1a");
//            Properties properties = new Properties();
//            System.out.println("EXPORT1b");
//            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
//            System.out.println("EXPORT1c");
//            ssrBuilder.applySettings(properties);
//            
//            SchemaExport export = new SchemaExport();
//            System.out.println("EXPORT1");
//            MetadataSources sources = new MetadataSources(ssrBuilder.build());
//            System.out.println("EXPORT2");
//            sources.addAnnotatedClass(RxModel.class);
//            System.out.println("EXPORT3");
//            for (String rxModelClass : rxModelClasses) {
//                sources.addAnnotatedClassName(rxModelClass);
//            }
//            System.out.println("EXPORT4");
//            Metadata metadata = sources.getMetadataBuilder().build();
//            System.out.println("EXPORT5");
//            export.create(EnumSet.of(TargetType.STDOUT), metadata);
//        }catch(Throwable t) {
//            t.printStackTrace();
//        }
//        System.out.println("EXPORT END");
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
