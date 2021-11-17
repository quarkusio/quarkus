package io.quarkus.oidc.runtime;

import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;

public class OidcConfigPropertySupplier implements Supplier<String> {

    private String oidcConfigProperty;
    private String defaultValue;

    public OidcConfigPropertySupplier() {

    }

    public OidcConfigPropertySupplier(String oidcConfigProperty) {
        this(oidcConfigProperty, null);
    }

    public OidcConfigPropertySupplier(String oidcConfigProperty, String defaultValue) {
        this.oidcConfigProperty = oidcConfigProperty;
        this.defaultValue = defaultValue;
    }

    @Override
    public String get() {
        if (defaultValue != null) {
            return ConfigProvider.getConfig().getOptionalValue(oidcConfigProperty, String.class).orElse(defaultValue);
        } else {
            return ConfigProvider.getConfig().getValue(oidcConfigProperty, String.class);
        }
    }

    public String getOidcConfigProperty() {
        return oidcConfigProperty;
    }

    public void setOidcConfigProperty(String oidcConfigProperty) {
        this.oidcConfigProperty = oidcConfigProperty;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

}
