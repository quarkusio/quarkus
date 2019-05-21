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
package io.quarkus.amazon.lambda.resteasy.adapter.test.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Request/response model
 */
public class MapResponseModel {
    private Map<String, String> values;

    public MapResponseModel() {
        this.values = new HashMap<>();
    }

    public void addValue(String key, String value) {
        this.values.put(key, value);
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }
}