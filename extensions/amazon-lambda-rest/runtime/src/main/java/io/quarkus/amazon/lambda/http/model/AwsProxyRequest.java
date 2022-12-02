/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package io.quarkus.amazon.lambda.http.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Default implementation of the request object from an API Gateway AWS_PROXY integration
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsProxyRequest {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String body;
    private String resource;
    private AwsProxyRequestContext requestContext;
    private MultiValuedTreeMap<String, String> multiValueQueryStringParameters;
    private Headers multiValueHeaders;
    private Map<String, String> pathParameters;
    private String httpMethod;
    private Map<String, String> stageVariables;
    private String path;
    private boolean isBase64Encoded;

    public AwsProxyRequest() {
        multiValueHeaders = new Headers();
        multiValueQueryStringParameters = new MultiValuedTreeMap<>();
        pathParameters = new HashMap<>();
        stageVariables = new HashMap<>();
    }

    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    @JsonIgnore
    public String getQueryString() {
        StringBuilder params = new StringBuilder("");

        if (this.getMultiValueQueryStringParameters() == null) {
            return "";
        }

        for (String key : this.getMultiValueQueryStringParameters().keySet()) {
            for (String val : this.getMultiValueQueryStringParameters().get(key)) {
                String separator = params.length() == 0 ? "?" : "&";

                params.append(separator + key + "=" + val);
            }
        }

        return params.toString();
    }

    public RequestSource getRequestSource() {
        if (getRequestContext() != null && getRequestContext().getElb() != null) {
            return RequestSource.ALB;
        }

        return RequestSource.API_GATEWAY;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public AwsProxyRequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(AwsProxyRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public MultiValuedTreeMap<String, String> getMultiValueQueryStringParameters() {
        return multiValueQueryStringParameters;
    }

    public void setMultiValueQueryStringParameters(
            MultiValuedTreeMap<String, String> multiValueQueryStringParameters) {
        this.multiValueQueryStringParameters = multiValueQueryStringParameters;
    }

    public Headers getMultiValueHeaders() {
        return multiValueHeaders;
    }

    public void setMultiValueHeaders(Headers multiValueHeaders) {
        this.multiValueHeaders = multiValueHeaders;
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Map<String, String> getStageVariables() {
        return stageVariables;
    }

    public void setStageVariables(Map<String, String> stageVariables) {
        this.stageVariables = stageVariables;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("isBase64Encoded")
    public boolean isBase64Encoded() {
        return isBase64Encoded;
    }

    public void setIsBase64Encoded(boolean base64Encoded) {
        isBase64Encoded = base64Encoded;
    }

    public static enum RequestSource {
        ALB,
        API_GATEWAY
    }
}
