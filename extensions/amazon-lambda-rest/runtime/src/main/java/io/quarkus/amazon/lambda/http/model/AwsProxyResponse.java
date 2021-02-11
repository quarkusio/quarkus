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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response object for an API Gateway method using AWS_PROXY integrations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsProxyResponse {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private int statusCode;
    private String statusDescription;
    private Map<String, String> headers;
    private Headers multiValueHeaders;
    private String body;
    private boolean isBase64Encoded;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public AwsProxyResponse() {

    }

    public AwsProxyResponse(int statusCode) {
        this(statusCode, null);
    }

    public AwsProxyResponse(int statusCode, Headers headers) {
        this(statusCode, headers, null);
    }

    public AwsProxyResponse(int statusCode, Headers headers, String body) {
        this.statusCode = statusCode;
        this.multiValueHeaders = headers;
        this.body = body;
    }

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    public void addHeader(String key, String value) {
        if (this.multiValueHeaders == null) {
            this.multiValueHeaders = new Headers();
        }

        this.multiValueHeaders.add(key, value);
    }

    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Headers getMultiValueHeaders() {
        return multiValueHeaders;
    }

    public void setMultiValueHeaders(Headers multiValueHeaders) {
        this.multiValueHeaders = multiValueHeaders;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @JsonProperty("isBase64Encoded")
    public boolean isBase64Encoded() {
        return isBase64Encoded;
    }

    public void setBase64Encoded(boolean base64Encoded) {
        isBase64Encoded = base64Encoded;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }
}
