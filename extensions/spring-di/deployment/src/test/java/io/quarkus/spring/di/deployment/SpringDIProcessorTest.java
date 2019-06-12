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
package io.quarkus.spring.di.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.deployment.util.IoUtil;

/**
 * @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a>
 */
class SpringDIProcessorTest {

    final SpringDIProcessor processor = new SpringDIProcessor();
    final IndexView index = getIndex(PrototypeService.class, RequestService.class,
            UndeclaredService.class, ConflictService.class, ConflictBean.class, RequestBean.class, UndeclaredBean.class,
            ConflictStereotypeBean.class, NamedBean.class, OverrideConflictStereotypeBean.class, ConflictNamedBean.class);

    @Test
    public void springStereotypeScopes() {
        final Map<DotName, Set<DotName>> scopes = processor.getStereotypeScopes(index);

        final Map<DotName, Set<DotName>> expected = new HashMap<>();
        expected.put(SpringDIProcessor.SPRING_COMPONENT, Collections.emptySet());
        expected.put(SpringDIProcessor.SPRING_SERVICE, Collections.emptySet());
        expected.put(SpringDIProcessor.SPRING_REPOSITORY, Collections.emptySet());
        expected.put(DotName.createSimple(PrototypeService.class.getName()),
                Collections.singleton(DotName.createSimple(Dependent.class.getName())));
        expected.put(DotName.createSimple(RequestService.class.getName()),
                Collections.singleton(DotName.createSimple(RequestScoped.class.getName())));
        expected.put(DotName.createSimple(UndeclaredService.class.getName()),
                Collections.emptySet());
        expected.put(DotName.createSimple(ConflictService.class.getName()),
                setOf(DotName.createSimple(Dependent.class.getName()), DotName.createSimple(RequestScoped.class.getName())));
        assertEquals(expected, scopes);
    }

    @Test
    public void getAnnotationsToAddConflictingScopesThrow() {
        final Map<DotName, Set<DotName>> scopes = processor.getStereotypeScopes(index);

        Assertions.assertThrows(DefinitionException.class, () -> {
            final ClassInfo target = index.getClassByName(DotName.createSimple(ConflictBean.class.getName()));
            processor.getAnnotationsToAdd(target, scopes, null);
        });

        Assertions.assertThrows(DefinitionException.class, () -> {
            final ClassInfo target = index
                    .getClassByName(DotName.createSimple(ConflictStereotypeBean.class.getName()));
            processor.getAnnotationsToAdd(target, scopes, null);
        });
    }

    @Test
    public void getAnnotationsToAddExplicitScopeOnConflictWorks() {
        final Map<DotName, Set<DotName>> scopes = processor.getStereotypeScopes(index);
        final ClassInfo target = index
                .getClassByName(DotName.createSimple(OverrideConflictStereotypeBean.class.getName()));

        final Set<AnnotationInstance> ret = processor.getAnnotationsToAdd(target, scopes, null);

        final Set<AnnotationInstance> expected = setOf(
                AnnotationInstance.create(DotName.createSimple(ApplicationScoped.class.getName()), target,
                        Collections.emptyList()));
        assertEquals(expected, ret);
    }

    @Test
    public void getAnnotationsToAdd() {
        final Map<DotName, Set<DotName>> scopes = processor.getStereotypeScopes(index);
        final ClassInfo target = index.getClassByName(DotName.createSimple(RequestBean.class.getName()));

        final Set<AnnotationInstance> ret = processor.getAnnotationsToAdd(target, scopes, null);

        final Set<AnnotationInstance> expected = setOf(
                AnnotationInstance.create(DotName.createSimple(RequestScoped.class.getName()), target,
                        Collections.emptyList()));
        assertEquals(expected, ret);
    }

    @Test
    public void getAnnotationsToAddDefaultsToSingleton() {
        final Map<DotName, Set<DotName>> scopes = processor.getStereotypeScopes(index);
        final ClassInfo target = index.getClassByName(DotName.createSimple(UndeclaredBean.class.getName()));

        final Set<AnnotationInstance> ret = processor.getAnnotationsToAdd(target, scopes, null);

        final Set<AnnotationInstance> expected = setOf(
                AnnotationInstance.create(DotName.createSimple(Singleton.class.getName()), target,
                        Collections.emptyList()));
        assertEquals(expected, ret);
    }

    @Test
    public void getAnnotationsToAddNamed() {
        final Map<DotName, Set<DotName>> scopes = processor.getStereotypeScopes(index);
        final ClassInfo target = index.getClassByName(DotName.createSimple(NamedBean.class.getName()));

        final Set<AnnotationInstance> ret = processor.getAnnotationsToAdd(target, scopes, null);

        final Set<AnnotationInstance> expected = setOf(
                AnnotationInstance.create(DotName.createSimple(Singleton.class.getName()), target,
                        Collections.emptyList()),
                AnnotationInstance.create(DotName.createSimple(Named.class.getName()), target,
                        Collections.singletonList(AnnotationValue.createStringValue("value", "named"))));
        assertEquals(expected, ret);
    }

    @Test
    public void getAnnotationsToAddMultipleNamesThrows() {
        final Map<DotName, Set<DotName>> scopes = processor.getStereotypeScopes(index);
        final ClassInfo target = index.getClassByName(DotName.createSimple(ConflictNamedBean.class.getName()));

        Assertions.assertThrows(DefinitionException.class, () -> {
            processor.getAnnotationsToAdd(target, scopes, null);
        });
    }

    private IndexView getIndex(final Class<?>... classes) {
        final Indexer indexer = new Indexer();
        for (final Class<?> clazz : classes) {
            final String className = clazz.getName();
            try (InputStream stream = IoUtil.readClass(getClass().getClassLoader(), className)) {
                final ClassInfo beanInfo = indexer.index(stream);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to index: " + className, e);
            }
        }
        return BeanArchives.buildBeanArchiveIndex(indexer.complete());
    }

    @SafeVarargs
    private static <T> Set<T> setOf(final T... vals) {
        final Set<T> ret = new LinkedHashSet<>();
        Collections.addAll(ret, vals);
        return ret;
    }

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Service
    @Scope(value = "prototype")
    public @interface PrototypeService {
    }

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Service
    @Scope(value = "request")
    public @interface RequestService {
    }

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Service
    public @interface UndeclaredService {
    }

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @PrototypeService
    @RequestService
    public @interface ConflictService {
    }

    @RequestService
    @PrototypeService
    public class ConflictBean {
    }

    @RequestService
    public class RequestBean {
    }

    @UndeclaredService
    public class UndeclaredBean {
    }

    @Service("named")
    public class NamedBean {
    }

    @Service("named")
    @Component("otherName")
    public class ConflictNamedBean {
    }

    @ConflictService
    public class ConflictStereotypeBean {
    }

    @ConflictService
    @Scope("application")
    public class OverrideConflictStereotypeBean {
    }
}