package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.shamrock.runtime.ObjectSubstitution;

/**
 * Used to capture object substitution information for non-serializable classes
 */
public final class ObjectSubstitutionBuildItem extends MultiBuildItem {
    /**
     * Holder to keep type info around for compiler
     * @param <F> - from class
     * @param <T> - to class
     */
    public static final class Holder<F,T> {
        public final Class<F> from;

        public final Class<T> to;

        public final Class<ObjectSubstitution<F, T>> substitution;
        public Holder(Class<F> from, Class<T> to, Class<ObjectSubstitution<F,T>> substitution) {
            this.from = from;
            this.to = to;
            this.substitution = substitution;

        }
    }
    public final Holder<?,?> holder;

    public <F, T> ObjectSubstitutionBuildItem(Class<F> from, Class<T> to, Class<ObjectSubstitution<F,T>> substitution) {
        holder = new Holder<>(from, to, substitution);
    }
    public ObjectSubstitutionBuildItem(Holder<?,?> holder) {
        this.holder = holder;
    }
    public Holder<?, ?> getHolder() {
        return holder;
    }
}
