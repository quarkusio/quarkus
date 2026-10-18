package io.quarkus.resteasy.test.security.inheritance;

public interface SubPaths {

    record SubPath(String classSubPathPrefix, String methodSubPath) {
    }

    String CLASS_PATH_ON_INTERFACE = "class-path-on-interface";
    String CLASS_PATH_ON_RESOURCE = "class-path-on-resource";
    String CLASS_PATH_ON_PARENT_RESOURCE = "class-path-on-parent-resource";

    String CLASS_SECURITY_ON_BASE = "class-security-on-base-";
    String CLASS_SECURITY_ON_PARENT = "class-security-on-parent-";
    String CLASS_SECURITY_ON_INTERFACE = "class-security-on-interface-";

    String IMPL_ON_BASE = "/impl-on-base-resource";
    String IMPL_ON_PARENT = "/impl-on-parent-resource";
    String IMPL_ON_INTERFACE = "/impl-on-interface";

    String SUB_DECLARED_ON = "/sub-resource-declared-on-";

    /**
     * Following 3 constants refer to where method like {@code @Path("sub") SubResource subResource} with JAX-RS
     * sub-resource declaring annotations are declared.
     */
    String SUB_DECLARED_ON_INTERFACE = SUB_DECLARED_ON + "interface";
    String SUB_DECLARED_ON_BASE = SUB_DECLARED_ON + "base";
    String SUB_DECLARED_ON_PARENT = SUB_DECLARED_ON + "parent";

    String SECURED_SUB_RESOURCE_ENDPOINT_PATH = "/secured";

    /**
     * Following 3 constants refer to where method like {@code @Override SubResource subResource() { return new SubResource();
     * }}
     * is implemented. That is whether actually invoked sub-resource endpoint is placed on a base, parent or an interface.
     */
    String SUB_IMPL_ON_BASE = "/sub-impl-on-base";
    String SUB_IMPL_ON_PARENT = "/sub-impl-on-parent";
    String SUB_IMPL_ON_INTERFACE = "/sub-impl-on-interface";

    String IMPL_METHOD_WITH_PATH = "/impl-met-with-path";
    String PARENT_METHOD_WITH_PATH = "/parent-met-with-path";
    String BASE_METHOD_WITH_PATH = "/base-met-with-path";
    String SUB_METHOD_WITH_PATH = "/sub-met-with-path";
    String INTERFACE_METHOD_WITH_PATH = "/interface-met-with-path";

    String CLASS_NO_ANNOTATION_PREFIX = "/class-no-annotation-";
    String CLASS_ROLES_ALLOWED_PREFIX = "/class-roles-allowed-";
    String CLASS_DENY_ALL_PREFIX = "/class-deny-all-";
    String CLASS_PERMIT_ALL_PREFIX = "/class-permit-all-";

    String NO_SECURITY_ANNOTATION_PATH = "/no-security-annotation";
    String METHOD_ROLES_ALLOWED_PATH = "/method-roles-allowed";
    String METHOD_DENY_ALL_PATH = "/method-deny-all";
    String METHOD_PERMIT_ALL_PATH = "/method-permit-all";
    String CLASS_ROLES_ALLOWED_PATH = "/class-roles-allowed";
    String CLASS_DENY_ALL_PATH = "/class-deny-all";
    String CLASS_PERMIT_ALL_PATH = "/class-permit-all";
    String CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH = "/class-deny-all-method-roles-allowed";
    String CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH = "/class-deny-all-method-permit-all";
    String CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH = "/class-permit-all-method-permit-all";

    SubPath NO_SECURITY_ANNOTATION = new SubPath(CLASS_NO_ANNOTATION_PREFIX, NO_SECURITY_ANNOTATION_PATH);
    SubPath METHOD_ROLES_ALLOWED = new SubPath(CLASS_NO_ANNOTATION_PREFIX, METHOD_ROLES_ALLOWED_PATH);
    SubPath METHOD_DENY_ALL = new SubPath(CLASS_NO_ANNOTATION_PREFIX, METHOD_DENY_ALL_PATH);
    SubPath METHOD_PERMIT_ALL = new SubPath(CLASS_NO_ANNOTATION_PREFIX, METHOD_PERMIT_ALL_PATH);
    SubPath CLASS_ROLES_ALLOWED = new SubPath(CLASS_ROLES_ALLOWED_PREFIX, CLASS_ROLES_ALLOWED_PATH);
    SubPath CLASS_DENY_ALL = new SubPath(CLASS_DENY_ALL_PREFIX, CLASS_DENY_ALL_PATH);
    SubPath CLASS_PERMIT_ALL = new SubPath(CLASS_PERMIT_ALL_PREFIX, CLASS_PERMIT_ALL_PATH);
    SubPath CLASS_DENY_ALL_METHOD_ROLES_ALLOWED = new SubPath(CLASS_DENY_ALL_PREFIX, CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH);
    SubPath CLASS_DENY_ALL_METHOD_PERMIT_ALL = new SubPath(CLASS_DENY_ALL_PREFIX, CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH);
    SubPath CLASS_PERMIT_ALL_METHOD_PERMIT_ALL = new SubPath(CLASS_PERMIT_ALL_PREFIX, CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH);
}
