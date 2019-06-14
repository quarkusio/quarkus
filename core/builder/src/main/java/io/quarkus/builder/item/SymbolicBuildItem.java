package io.quarkus.builder.item;

/**
 * The symbolic build item.
 */
public final class SymbolicBuildItem extends NamedBuildItem<Enum<?>> {

    private static final SymbolicBuildItem INSTANCE = new SymbolicBuildItem();

    private SymbolicBuildItem() {
    }

    /**
     * Get the singleton instance.
     *
     * @return the singleton instance (not {@code null})
     */
    public static SymbolicBuildItem getInstance() {
        return INSTANCE;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this;
    }

    @Override
    public String toString() {
        return "symbolic";
    }
}
