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
package io.quarkus.smallrye.restclient.runtime;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.specimpl.UnmodifiableMultivaluedMap;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 2/28/19
 */
public class IncomingHeadersProvider implements io.smallrye.restclient.header.IncomingHeadersProvider {
    public static final UnmodifiableMultivaluedMap<String, String> EMPTY_MAP = new UnmodifiableMultivaluedMap<>(
            new MultivaluedHashMap<>());

    /**
     * @return headers incoming in the JAX-RS request, if any
     */
    @Override
    public MultivaluedMap<String, String> getIncomingHeaders() {
        MultivaluedMap<String, String> result = null;

        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.peekInstance();
        if (providerFactory != null) {
            HttpRequest request = (HttpRequest) providerFactory.getContextData(HttpRequest.class);
            if (request != null) {
                result = request.getHttpHeaders().getRequestHeaders();
            }
        }
        return result == null
                ? EMPTY_MAP
                : result;
    }
}