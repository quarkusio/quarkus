package io.quarkus.amazon.lambda.runtime;

import com.amazonaws.services.lambda.runtime.Client;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

public class ClientImpl implements Client {
    private String id;
    private String title;
    private String versionName;
    private String versionCode;
    private String packageName;

    @JsonGetter("client_id")
    public String getId() {
        return id;
    }

    @JsonSetter("client_id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonGetter("app_title")
    public String getTitle() {
        return title;
    }

    @JsonSetter("app_title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonGetter("app_version_name")
    public String getVersionName() {
        return versionName;
    }

    @JsonSetter("app_version_name")
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    @JsonGetter("app_version_code")
    public String getVersionCode() {
        return versionCode;
    }

    @JsonSetter("app_version_code")
    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    @JsonGetter("app_package_name")
    public String getPackageName() {
        return packageName;
    }

    @JsonSetter("app_package_name")
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    @JsonIgnore
    public String getInstallationId() {
        return id;
    }

    @Override
    @JsonIgnore
    public String getAppTitle() {
        return title;
    }

    @Override
    @JsonIgnore
    public String getAppVersionName() {
        return versionName;
    }

    @Override
    @JsonIgnore
    public String getAppVersionCode() {
        return versionCode;
    }

    @Override
    @JsonIgnore
    public String getAppPackageName() {
        return packageName;
    }
}
