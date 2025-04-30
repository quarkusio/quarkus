package io.quarkus.resteasy.test.security.inheritance;

import jakarta.ws.rs.Path;

public enum SecurityAnnotation {
    NONE(SubPaths.NO_SECURITY_ANNOTATION, false, null, false),
    METHOD_ROLES_ALLOWED(SubPaths.METHOD_ROLES_ALLOWED, false, "admin", false),
    METHOD_DENY_ALL(SubPaths.METHOD_DENY_ALL, true, null, false),
    METHOD_PERMIT_ALL(SubPaths.METHOD_PERMIT_ALL, false, null, false),
    CLASS_ROLES_ALLOWED(SubPaths.CLASS_ROLES_ALLOWED, false, "admin", true),
    CLASS_DENY_ALL(SubPaths.CLASS_DENY_ALL, true, null, true),
    CLASS_PERMIT_ALL(SubPaths.CLASS_PERMIT_ALL, false, null, true),
    CLASS_PERMIT_ALL_METHOD_PERMIT_ALL(SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL, false, null, true),
    // class is annotated with the @DenyAll, but method level annotation must have priority, therefore we set denyAll=false
    CLASS_DENY_ALL_METHOD_ROLES_ALLOWED(SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED, false, "admin", true),
    CLASS_DENY_ALL_METHOD_PERMIT_ALL(SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL, false, null, true);

    static final String PATH_SEPARATOR = "/";

    private final SubPaths.SubPath subPath;
    private final String allowedRole;
    private final boolean isClassSecurityAnnotation;
    private final boolean denyAll;

    SecurityAnnotation(SubPaths.SubPath subPath, boolean denyAll, String allowedRole, boolean isClassSecurityAnnotation) {
        this.subPath = subPath;
        this.denyAll = denyAll;
        this.allowedRole = allowedRole;
        this.isClassSecurityAnnotation = isClassSecurityAnnotation;
    }

    private String toSecurityAnnInfix(String classSecurityOn) {
        return isClassSecurityAnnotation ? classSecurityOn : "";
    }

    boolean hasSecurityAnnotation() {
        return this != NONE;
    }

    boolean denyAll() {
        return denyAll;
    }

    boolean endpointSecured() {
        return denyAll || allowedRole != null;
    }

    /**
     * @param basePath path common for all {@link this} annotations
     * @param classSecurityOn whether class-level annotation is on interface, parent or base
     * @return request path
     */
    String assemblePath(String basePath, String classSecurityOn) {
        return subPath.classSubPathPrefix() + toSecurityAnnInfix(classSecurityOn) + basePath + subPath.methodSubPath();
    }

    String assemblePath(String basePath) {
        return subPath.classSubPathPrefix() + basePath + subPath.methodSubPath();
    }

    String assembleNotFoundPath(String basePath) {
        return subPath.classSubPathPrefix() + basePath;
    }

    /**
     * @return endpoint method-level {@link Path#value()}
     */
    String methodSubPath(String basePath, String classSecurityOn) {
        var path = assemblePath(basePath, classSecurityOn);
        return path.substring(path.indexOf(PATH_SEPARATOR, 1) + 1);
    }

    String methodSubPath(String basePath) {
        var path = assemblePath(basePath);
        return path.substring(path.indexOf(PATH_SEPARATOR, 1) + 1);
    }
}