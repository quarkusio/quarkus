package io.quarkus.runtime.configuration;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.ProfileConfigSourceInterceptor;

class QuarkusProfileConfigSourceInterceptor extends ProfileConfigSourceInterceptor {
    private final String profile;

    public QuarkusProfileConfigSourceInterceptor(final String profile) {
        super(profile);
        this.profile = profile;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (profile != null) {
            final String normalizeName = normalizeName(name);
            final ConfigValue profileValue = context.proceed("%" + profile + "." + normalizeName);
            if (profileValue != null) {
                return profileValue.withName(normalizeName);
            }
        }

        return context.proceed(name);
    }

    private String normalizeName(final String name) {
        return name.startsWith("%" + profile + ".") ? name.substring(profile.length() + 2) : name;
    }
}
