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

package org.jboss.shamrock.example.transaction;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/txn")
public class TransactionResource {

    @Inject
    UserTransaction userTransaction;

    @Inject
    TransactionSynchronizationRegistry trs;

    @GET
    public boolean tryTxn() throws Exception {
        final AtomicBoolean res = new AtomicBoolean();
        userTransaction.begin();
        trs.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                res.set(true);
            }

            @Override
            public void afterCompletion(int status) {

            }
        });
        userTransaction.commit();

        return res.get();
    }

}
