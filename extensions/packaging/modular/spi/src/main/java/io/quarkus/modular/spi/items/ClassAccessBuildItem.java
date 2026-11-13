package io.quarkus.modular.spi.items;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Declares access between classes whose modules are resolved by modular packaging after generated class placement.
 * Grants apply to the target class's entire package, and do not bypass Java member visibility.
 */
public final class ClassAccessBuildItem extends MultiBuildItem {
    public enum Kind {
        DIRECT,
        REFLECTIVE
    }

    private final String callerClass;
    private final String targetClass;
    private final Kind kind;

    private ClassAccessBuildItem(String callerClass, String targetClass, Kind kind) {
        this.callerClass = Assert.checkNotEmptyParam("callerClass", callerClass).replace('/', '.');
        this.targetClass = Assert.checkNotEmptyParam("targetClass", targetClass).replace('/', '.');
        this.kind = kind;
    }

    /** Requests class loading linkage, readability, and a qualified export for direct bytecode access. */
    public static ClassAccessBuildItem direct(String callerClass, String targetClass) {
        return new ClassAccessBuildItem(callerClass, targetClass, Kind.DIRECT);
    }

    /**
     * Requests readability and a qualified open for deep reflection. This does not request class loading linkage:
     * the caller must already have the reflected class or member, or obtain it through an appropriate class loader.
     */
    public static ClassAccessBuildItem reflective(String callerClass, String declaringClass) {
        return new ClassAccessBuildItem(callerClass, declaringClass, Kind.REFLECTIVE);
    }

    public String callerClass() {
        return callerClass;
    }

    public String targetClass() {
        return targetClass;
    }

    public Kind kind() {
        return kind;
    }
}
