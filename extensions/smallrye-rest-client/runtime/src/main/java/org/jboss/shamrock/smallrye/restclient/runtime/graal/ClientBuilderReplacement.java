/*
 *  Copyright (c) 2019 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.quarkus.smallrye.restclient.runtime.graal;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ClientBuilder.class)
final class ClientBuilderReplacement {

    @Substitute
    public static ClientBuilder newBuilder() {
        return new ResteasyClientBuilderImpl();
    }
}
