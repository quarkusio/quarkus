package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

public class ViewIgnoreBean {

    @JsonView(GeneratedViews.Public.class)
    private String publicField;

    @JsonView(GeneratedViews.Private.class)
    private int privateField;

    @JsonIgnore
    private String ignoredField;

    public String getPublicField() {
        return publicField;
    }

    public void setPublicField(String publicField) {
        this.publicField = publicField;
    }

    public int getPrivateField() {
        return privateField;
    }

    public void setPrivateField(int privateField) {
        this.privateField = privateField;
    }

    public String getIgnoredField() {
        return ignoredField;
    }

    public void setIgnoredField(String ignoredField) {
        this.ignoredField = ignoredField;
    }
}
