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

package io.quarkus.undertow.runtime.graal;

import java.io.Closeable;

import org.xnio.IoUtils;
import org.xnio.Xnio;
import org.xnio.management.XnioProviderMXBean;
import org.xnio.management.XnioServerMXBean;
import org.xnio.management.XnioWorkerMXBean;
import org.xnio.nio.NioXnioProvider;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.xnio.Xnio")
final class XnioSubstitution {

    @Substitute
    public static Xnio getInstance() {
        return new NioXnioProvider().getInstance();
    }

    @Substitute
    public static Xnio getInstance(final ClassLoader classLoader) {
        return new NioXnioProvider().getInstance();
    }

    @Substitute
    protected static Closeable register(XnioProviderMXBean providerMXBean) {
        return IoUtils.nullCloseable();
    }

    @Substitute
    protected static Closeable register(XnioWorkerMXBean workerMXBean) {
        return IoUtils.nullCloseable();
    }

    @Substitute
    protected static Closeable register(XnioServerMXBean serverMXBean) {
        return IoUtils.nullCloseable();
    }
}
