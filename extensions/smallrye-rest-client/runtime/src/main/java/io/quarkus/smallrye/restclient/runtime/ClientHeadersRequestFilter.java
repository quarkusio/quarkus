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

import static io.smallrye.restclient.utils.ListCastUtils.castToListOfStrings;

import java.lang.reflect.Method;
import java.util.*;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.resteasy.core.ResteasyContext;

import io.smallrye.restclient.header.ClientHeaderProvider;
import io.smallrye.restclient.header.ClientHeaderProviders;
import io.smallrye.restclient.header.HttpHeadersContextProvider;
import io.smallrye.restclient.header.IncomingHeadersProvider;
import io.smallrye.restclient.utils.ClientRequestContextUtils;

/**
 * First the headers from `@ClientHeaderParam` annotations are applied,
 * they can be overwritten by JAX-RS `@HeaderParam` (coming in the `requestContext`)
 *
 * Then, if a `ClientHeadersFactory` is defined, all the headers, together with headers from `IncomingHeadersProvider`,
 * are passed to it and it can overwrite them.
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 1/12/19
 */
@Priority(Integer.MIN_VALUE)
public class ClientHeadersRequestFilter implements ClientRequestFilter {

    private static final IncomingHeadersProvider noIncomingHeadersProvider = MultivaluedHashMap::new;

    private static final IncomingHeadersProvider incomingHeadersProvider;

    static {
        incomingHeadersProvider = initializeProvider();
    }

    private static IncomingHeadersProvider initializeProvider() {
        ServiceLoader<IncomingHeadersProvider> providerLoader = ServiceLoader.load(IncomingHeadersProvider.class);

        Iterator<IncomingHeadersProvider> providers = providerLoader.iterator();

        if (!providers.hasNext()) {
            return noIncomingHeadersProvider;
        }

        IncomingHeadersProvider result = providers.next();
        if (providers.hasNext()) {
            throw new RuntimeException("Multiple " + IncomingHeadersProvider.class.getCanonicalName() + "'s " +
                    "registered, expecting at most one.");
        }

        return result;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        Method method = ClientRequestContextUtils.getMethod(requestContext);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        Optional<ClientHeaderProvider> handler = ClientHeaderProviders.getProvider(method);
        handler.ifPresent(h -> h.addHeaders(headers));

        Optional<ClientHeadersFactory> factory = ClientHeaderProviders.getFactory(method.getDeclaringClass());

        requestContext.getHeaders().forEach(
                (key, values) -> headers.put(key, castToListOfStrings(values)));

        factory.map(f -> updateHeaders(headers, f))
                .orElse(headers)
                .forEach(
                        (key, values) -> requestContext.getHeaders().put(key, castToListOfObjects(values)));

        ResteasyContext.getContextDataMap().put(HttpHeaders.class, new HttpHeadersContextProvider(requestContext));
    }

    private MultivaluedMap<String, String> updateHeaders(MultivaluedMap<String, String> headers, ClientHeadersFactory factory) {
        return factory.update(incomingHeadersProvider.getIncomingHeaders(), headers);
    }

    private static List<Object> castToListOfObjects(List<String> values) {
        return new ArrayList<>(values);
    }
}
