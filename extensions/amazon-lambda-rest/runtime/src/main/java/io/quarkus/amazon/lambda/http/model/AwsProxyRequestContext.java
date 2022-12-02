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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The API Gateway request context object. This is used by the default implementation of the AWS_PROXY integration type.
 * All of the values are part of the API Gateway $context variable so this object could be reused with custom request
 * readers.
 *
 * @see AwsProxyRequest
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsProxyRequestContext {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String resourceId;
    private String apiId;
    private String resourcePath;
    private String httpMethod;
    private String requestId;
    private String extendedRequestId;
    private String accountId;
    private ApiGatewayRequestIdentity identity;
    private ApiGatewayAuthorizerContext authorizer;
    private String stage;
    private String path;
    private String protocol;
    private String requestTime;
    private long requestTimeEpoch;

    private AlbContext elb;

    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getExtendedRequestId() {
        return extendedRequestId;
    }

    public void setExtendedRequestId(String extendedRequestId) {
        this.extendedRequestId = extendedRequestId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public ApiGatewayRequestIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(ApiGatewayRequestIdentity identity) {
        this.identity = identity;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ApiGatewayAuthorizerContext getAuthorizer() {
        return authorizer;
    }

    public void setAuthorizer(ApiGatewayAuthorizerContext authorizer) {
        this.authorizer = authorizer;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }

    public long getRequestTimeEpoch() {
        return requestTimeEpoch;
    }

    public void setRequestTimeEpoch(long requestTimeEpoch) {
        this.requestTimeEpoch = requestTimeEpoch;
    }

    public AlbContext getElb() {
        return elb;
    }

    public void setElb(AlbContext elb) {
        this.elb = elb;
    }
}
