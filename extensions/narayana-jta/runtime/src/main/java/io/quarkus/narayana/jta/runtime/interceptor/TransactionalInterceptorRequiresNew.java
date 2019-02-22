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

package io.quarkus.narayana.jta.runtime.interceptor;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

/**
 * @author paul.robinson@redhat.com 25/05/2013
 */

@Interceptor
@Transactional(Transactional.TxType.REQUIRES_NEW)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class TransactionalInterceptorRequiresNew extends TransactionalInterceptorBase {
    public TransactionalInterceptorRequiresNew() {
        super(false);
    }

    @Override
    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        return super.intercept(ic);
    }

    @Override
    protected Object doIntercept(TransactionManager tm, Transaction tx, InvocationContext ic) throws Exception {
        if (tx != null) {
            tm.suspend();
            try {
                return invokeInOurTx(ic, tm);
            } finally {
                tm.resume(tx);
            }
        } else {
            return invokeInOurTx(ic, tm);
        }
    }
}
