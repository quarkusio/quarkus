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

package io.quarkus.hibernate.orm;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TransactionRequiredException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class NoTransactionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Address.class)
                    .addAsResource("application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    public void testNTransaction() {
        Arc.container().requestContext().activate();
        try {
            Assertions.assertThrows(TransactionRequiredException.class, () -> {
                Address a = new Address("test");
                entityManager.persist(a);
            });
        } finally {
            Arc.container().requestContext().terminate();
        }
    }

}
