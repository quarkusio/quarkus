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

/**
 * Default error response model. This object is used by the <code>AwsProxyExceptionHandler</code> object.
 */
public class ErrorModel {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String message;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public ErrorModel() {
        this(null);
    }

    public ErrorModel(String message) {
        this.message = message;
    }

    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
