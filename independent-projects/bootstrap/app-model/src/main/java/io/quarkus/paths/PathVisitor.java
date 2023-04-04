package io.quarkus.paths;

/**
 * {@link PathTree} path visitor
 */
public interface PathVisitor {

    /**
     * Called to visit a path when walking a path tree or when a caller
     * requested to visit a specific path in a tree. In the latter case
     * if the requested path does not exist, the {@code visit} argument
     * will be null and it'll be up to the caller how to handle that,
     * i.e. whether to throw a path not found exception or return silently.
     *
     * @param visit visit object or null, in case the requested path does not exist
     */
    void visitPath(PathVisit visit);
}
