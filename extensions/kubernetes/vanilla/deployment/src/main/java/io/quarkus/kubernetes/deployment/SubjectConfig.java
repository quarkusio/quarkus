package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SubjectConfig {

    /**
     * The "name" resource to use by the Subject element in the generated Role Binding resource.
     */
    @ConfigItem
    public Optional<String> name;

    /**
     * The "kind" resource to use by the Subject element in the generated Role Binding resource.
     * By default, it uses the "ServiceAccount" kind.
     */
    @ConfigItem(defaultValue = "ServiceAccount")
    public String kind;

    /**
     * The "apiGroup" resource that matches with the "kind" property. By default, it's empty.
     */
    @ConfigItem
    public Optional<String> apiGroup;

    /**
     * The "namespace" resource to use by the Subject element in the generated Role Binding resource.
     * By default, it will use the same as provided in the generated resources.
     */
    @ConfigItem
    public Optional<String> namespace;
}
