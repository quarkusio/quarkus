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

package io.quarkus.arc.test.build.extension.validator;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class BeanDeploymentValidatorTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Alpha.class).beanRegistrars(new TestRegistrar())
            .beanDeploymentValidators(new TestValidator()).build();

    @Test
    public void testValidator() {
        assertTrue(Arc.container().instance(Alpha.class).get().getStrings().isEmpty());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext registrationContext) {
            registrationContext.configure(List.class).types(EmptyStringListCreator.listStringType()).creator(EmptyStringListCreator.class).done();
        }

    }

    static class TestValidator implements BeanDeploymentValidator {

        @Override
        public void validate(ValidationContext validationContext) {
            assertTrue(validationContext.get(Key.BEANS)
                    .stream()
                    .anyMatch(b -> b.isClassBean() && b.getBeanClass()
                            .toString()
                            .equals(Alpha.class.getName())));
            assertTrue(validationContext.get(Key.BEANS)
                    .stream()
                    .anyMatch(b -> b.isSynthetic() && b.getTypes()
                            .contains(EmptyStringListCreator.listStringType())));
            assertTrue(validationContext.get(Key.OBSERVERS)
                    .stream()
                    .anyMatch(o -> o.getObservedType()
                            .equals(Type.create(DotName.createSimple(Object.class.getName()), Kind.CLASS))));
            // We do not test a validation problem - ArcTestContainer rule would fail
        }

    }

    @ApplicationScoped
    static class Alpha {

        @Inject
        private List<String> strings;

        void observeAppContextInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        }

        List<String> getStrings() {
            return strings;
        }

    }

    public static class EmptyStringListCreator implements BeanCreator<List<String>> {

        static Type listStringType() {
            Type[] args = new Type[1];
            args[0] = Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS);
            return ParameterizedType.create(DotName.createSimple(List.class.getName()), args, null);
        }

        @Override
        public List<String> create(CreationalContext<List<String>> creationalContext, Map<String, Object> params) {
            return Collections.emptyList();
        }

    }

}
