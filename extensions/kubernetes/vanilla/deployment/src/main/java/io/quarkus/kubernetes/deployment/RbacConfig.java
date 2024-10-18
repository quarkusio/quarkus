package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface RbacConfig {
    /**
     * List of roles to generate.
     */
    Map<String, RoleConfig> roles();

    /**
     * List of cluster roles to generate.
     */
    Map<String, ClusterRoleConfig> clusterRoles();

    /**
     * List of service account resources to generate.
     */
    Map<String, ServiceAccountConfig> serviceAccounts();

    /**
     * List of role bindings to generate.
     */
    Map<String, RoleBindingConfig> roleBindings();

    /**
     * List of cluster role bindings to generate.
     */
    Map<String, ClusterRoleBindingConfig> clusterRoleBindings();

    interface RoleConfig {
        /**
         * The name of the role.
         */
        Optional<String> name();

        /**
         * The namespace of the role.
         */
        Optional<String> namespace();

        /**
         * Labels to add into the Role resource.
         */
        @ConfigDocMapKey("label-name")
        Map<String, String> labels();

        /**
         * Policy rules of the Role resource.
         */
        Map<String, PolicyRuleConfig> policyRules();
    }

    interface ClusterRoleConfig {
        /**
         * The name of the cluster role.
         */
        Optional<String> name();

        /**
         * Labels to add into the ClusterRole resource.
         */
        @ConfigDocMapKey("label-name")
        Map<String, String> labels();

        /**
         * Policy rules of the ClusterRole resource.
         */
        Map<String, PolicyRuleConfig> policyRules();
    }

    interface ServiceAccountConfig {
        /**
         * The name of the service account.
         */
        Optional<String> name();

        /**
         * The namespace of the service account.
         */
        Optional<String> namespace();

        /**
         * Labels of the service account.
         */
        @ConfigDocMapKey("label-name")
        Map<String, String> labels();

        /**
         * If true, this service account will be used in the generated Deployment resource.
         */
        Optional<Boolean> useAsDefault();

        default boolean isUseAsDefault() {
            return useAsDefault().orElse(false);
        }
    }

    interface RoleBindingConfig {
        /**
         * Name of the RoleBinding resource to be generated. If not provided, it will use the application name plus the role
         * ref name.
         */
        Optional<String> name();

        /**
         * Labels to add into the RoleBinding resource.
         */
        @ConfigDocMapKey("label-name")
        Map<String, String> labels();

        /**
         * The name of the Role resource to use by the RoleRef element in the generated Role Binding resource.
         * By default, it's "view" role name.
         */
        Optional<String> roleName();

        /**
         * If the Role sets in the `role-name` property is cluster wide or not.
         */
        Optional<Boolean> clusterWide();

        /**
         * List of subjects elements to use in the generated RoleBinding resource.
         */
        Map<String, SubjectConfig> subjects();
    }

    interface ClusterRoleBindingConfig {
        /**
         * Name of the ClusterRoleBinding resource to be generated. If not provided, it will use the application name plus the
         * role
         * ref name.
         */
        Optional<String> name();

        /**
         * Labels to add into the RoleBinding resource.
         */
        @ConfigDocMapKey("label-name")
        Map<String, String> labels();

        /**
         * The name of the ClusterRole resource to use by the RoleRef element in the generated ClusterRoleBinding resource.
         */
        String roleName();

        /**
         * List of subjects elements to use in the generated ClusterRoleBinding resource.
         */
        Map<String, SubjectConfig> subjects();
    }

    interface PolicyRuleConfig {
        /**
         * API groups of the policy rule.
         */
        Optional<List<String>> apiGroups();

        /**
         * Non resource URLs of the policy rule.
         */
        Optional<List<String>> nonResourceUrls();

        /**
         * Resource names of the policy rule.
         */
        Optional<List<String>> resourceNames();

        /**
         * Resources of the policy rule.
         */
        Optional<List<String>> resources();

        /**
         * Verbs of the policy rule.
         */
        Optional<List<String>> verbs();
    }

    interface SubjectConfig {
        /**
         * The "name" resource to use by the Subject element in the generated Role Binding resource.
         */
        Optional<String> name();

        /**
         * The "kind" resource to use by the Subject element in the generated Role Binding resource.
         * By default, it uses the "ServiceAccount" kind.
         */
        @WithDefault("ServiceAccount")
        String kind();

        /**
         * The "apiGroup" resource that matches with the "kind" property. By default, it's empty.
         */
        Optional<String> apiGroup();

        /**
         * The "namespace" resource to use by the Subject element in the generated Role Binding resource.
         * By default, it will use the same as provided in the generated resources.
         */
        Optional<String> namespace();
    }
}
