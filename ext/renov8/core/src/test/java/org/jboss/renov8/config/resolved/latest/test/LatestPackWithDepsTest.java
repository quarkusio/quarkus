/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.renov8.config.resolved.latest.test;

import org.jboss.renov8.PackLocation;
import org.jboss.renov8.config.DistConfig;
import org.jboss.renov8.config.PackConfig;
import org.jboss.renov8.config.resolved.test.ResolvedSpecTestBase;
import org.jboss.renov8.test.TestPack;

/**
 *
 * @author Alexey Loubyansky
 */
public class LatestPackWithDepsTest extends ResolvedSpecTestBase {

    private static final PackLocation A_1 = location("A", "1");
    private static final PackLocation B_1 = location("B", "1");
    private static final PackLocation B_2 = location("B", "2");
    private static final PackLocation C_1 = location("C", "1");
    private static final PackLocation C_2 = location("C", "2");
    private static final PackLocation D_1 = location("D", "1");
    private static final PackLocation E_1 = location("E", "1");

    private static final PackLocation F_1 = location("F", "1");
    private static final PackLocation F_2 = location("F", "2");

    private TestPack A_1_SPEC;
    private TestPack B_2_SPEC;
    private TestPack C_2_SPEC;
    private TestPack E_1_SPEC;
    private TestPack F_2_SPEC;

    @Override
    protected void createPacks() throws Exception {

        A_1_SPEC = createPack(TestPack.builder(A_1)
                .addDependency(PackConfig.forLocation(B_1))
                .addDependency(PackConfig.forLocation(C_1))
                .build());

        createPack(TestPack.builder(B_1)
                .addDependency(PackConfig.forLocation(D_1))
                .build());

        B_2_SPEC = createPack(TestPack.builder(B_2)
                .addDependency(PackConfig.forLocation(E_1))
                .build());

        createPack(TestPack.builder(C_1)
                .build());
        C_2_SPEC = createPack(TestPack.builder(C_2)
                .build());

        createPack(TestPack.builder(D_1)
                .build());

        E_1_SPEC = createPack(TestPack.builder(E_1)
                .build());

        createPack(TestPack.builder(F_1)
                .build());

        F_2_SPEC = createPack(TestPack.builder(F_2)
                .addDependency(C_2)
                .build());
    }

    @Override
    protected String[] updateProducers() {
        return new String[] {"B", "F"};
    }

    @Override
    protected DistConfig distConfig() {
        return DistConfig.builder()
                .addPack(PackConfig.forLocation(A_1))
                .addPack(PackConfig.forLocation(F_2))
                .addPack(PackConfig.forLocation(C_2))
                .build();
    }

    @Override
    protected DistConfig resolvedConfig() {
        return DistConfig.builder()
                .addPack(PackConfig.forLocation(A_1))
                .addPack(PackConfig.forLocation(F_2))
                .addPack(PackConfig.forLocation(C_2))
                .addPack(PackConfig.forTransitive(B_2))
                .build();
    }

    @Override
    protected TestPack[] resolvedPacks() {
        return new TestPack[] {
                E_1_SPEC,

                B_2_SPEC,

                C_2_SPEC,

                A_1_SPEC,

                F_2_SPEC
        };
    }
}
