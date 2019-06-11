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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

public class RestClientBase {

    private static final String MP_REST = "mp-rest";
    public static final String REST_URL_FORMAT = "%s/" + MP_REST + "/url";
    public static final String REST_URI_FORMAT = "%s/" + MP_REST + "/uri";

    private final Class<?> proxyType;
    private final String baseUriFromAnnotation;

    private final Config config;

    public RestClientBase(Class<?> proxyType, String baseUriFromAnnotation) {
        this.proxyType = proxyType;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.config = ConfigProvider.getConfig();
    }

    public Object create() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        String baseUrl = getBaseUrl();
        try {
            return builder.baseUrl(new URL(baseUrl)).build(proxyType);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The value of URL was invalid " + baseUrl, e);
        } catch (Exception e) {
            if ("com.oracle.svm.core.jdk.UnsupportedFeatureError".equals(e.getClass().getCanonicalName())) {
                throw new IllegalArgumentException(baseUrl
                        + " requires SSL support but it is disabled. You probably have set quarkus.ssl.native to false.");
            }
            throw e;
        }
    }

    private String getBaseUrl() {
        String propertyName = String.format(REST_URI_FORMAT, proxyType.getName());
        Optional<String> propertyOptional = config.getOptionalValue(propertyName, String.class);
        if (!propertyOptional.isPresent()) {
            propertyName = String.format(REST_URL_FORMAT, proxyType.getName());
            propertyOptional = config.getOptionalValue(propertyName, String.class);
        }
        if (((baseUriFromAnnotation == null) || baseUriFromAnnotation.isEmpty())
                && !propertyOptional.isPresent()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Unable to determine the proper baseUrl/baseUri. Consider registering using @RegisterRestClient(baseUri=\"someuri\"), or by adding '%s' to your Quarkus configuration",
                            propertyName));
        }
        return propertyOptional.orElse(baseUriFromAnnotation);
    }
}
