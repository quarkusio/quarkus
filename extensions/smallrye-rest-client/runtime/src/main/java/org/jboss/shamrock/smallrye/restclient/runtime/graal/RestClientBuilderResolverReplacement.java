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

package org.jboss.shamrock.smallrye.restclient.runtime.graal;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.jboss.shamrock.smallrye.restclient.runtime.BuilderResolver;

@TargetClass(RestClientBuilderResolver.class)
final class RestClientBuilderResolverReplacement {

    @Substitute
    private static RestClientBuilderResolver loadSpi(ClassLoader cl) {
        return new BuilderResolver();
    }
}
