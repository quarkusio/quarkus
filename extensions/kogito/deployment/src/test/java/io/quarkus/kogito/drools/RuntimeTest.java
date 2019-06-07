/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.kogito.drools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import javax.inject.Inject;

import org.drools.modelcompiler.KieRuntimeBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kie.api.runtime.KieSession;

import io.quarkus.test.QuarkusUnitTest;

public class RuntimeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Person.class, Result.class)
                    .addAsResource("META-INF" + File.separator + "kmodule.xml", "src/main/resources/META-INF/kmodule.xml")
                    .addAsResource(
                            "org" + File.separator + "drools" + File.separator + "simple" + File.separator + "candrink"
                                    + File.separator + "CanDrink.drl",
                            "src/main/resources/org/drools/simple/candrink/CanDrink.drl")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    KieRuntimeBuilder runtimeBuilder;

    @Test
    public void testRuleEvaluation() {
        KieSession ksession = runtimeBuilder.newKieSession();

        Result result = new Result();
        ksession.insert(result);
        ksession.insert(new Person("Mark", 17));
        ksession.fireAllRules();

        assertEquals("Mark can NOT drink", result.toString());
    }
}
