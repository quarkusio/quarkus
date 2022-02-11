package com.example.postgresql.devservice.profile;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServiceCustomPortProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("quarkus.datasource.devservices.port", "5432");
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }

}
