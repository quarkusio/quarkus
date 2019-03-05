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

package io.quarkus.scheduler.runtime.graal;

import java.rmi.RemoteException;

import org.quartz.core.RemotableQuartzScheduler;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.quartz.core.QuartzScheduler")
final class Target_org_quartz_core_QuartzScheduler {

    @Substitute
    private void bind() throws RemoteException {
    }

    @Substitute
    private void unBind() throws RemoteException {
    }

    @Substitute
    private void registerJMX() throws Exception {
    }

    @Substitute
    private void unregisterJMX() throws Exception {
    }
}

@TargetClass(className = "org.quartz.impl.RemoteScheduler")
final class Target_org_quartz_impl_RemoteScheduler {

    @Substitute
    protected RemotableQuartzScheduler getRemoteScheduler() {
        return null;
    }

}

final class QuartzSubstitutions {
}
