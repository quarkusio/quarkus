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

package io.quarkus.arc.test.build.extension.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.junit.Rule;
import org.junit.Test;

public class BeanRegistrarTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(UselessBean.class).beanRegistrars(new TestRegistrar()).build();

    @Test
    public void testSyntheticBean() {
        assertEquals(Integer.valueOf(152), Arc.container().instance(Integer.class).get());
        assertEquals("Hello Frantisek!", Arc.container().instance(String.class).get());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public boolean initialize(BuildContext buildContext) {
            assertTrue(buildContext.get(Key.INDEX).getKnownClasses().stream().anyMatch(cl -> cl.name().toString().equals(UselessBean.class.getName())));
            return true;
        }

        @Override
        public void register(RegistrationContext registrationContext) {
            // Verify that the class bean was registered
            assertTrue(registrationContext.get(Key.BEANS).stream()
                    .anyMatch(b -> b.isClassBean() && b.getBeanClass().toString().equals(UselessBean.class.getName())));

            BeanConfigurator<Integer> integerConfigurator = registrationContext.configure(Integer.class);
            integerConfigurator.types(Integer.class).creator(mc -> {
                ResultHandle ret = mc.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class), mc.load(152));
                mc.returnValue(ret);
            });
            integerConfigurator.done();

            registrationContext.configure(String.class).types(String.class).param("name", "Frantisek").creator(StringCreator.class).done();
        }

    }

    public static class StringCreator implements BeanCreator<String> {

        @Override
        public String create(CreationalContext<String> creationalContext, Map<String, Object> params) {
            return "Hello " + params.get("name") + "!";
        }

    }

    @ApplicationScoped
    static class UselessBean {

    }

}
