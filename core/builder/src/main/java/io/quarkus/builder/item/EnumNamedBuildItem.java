package io.quarkus.builder.item;

/**
 * A build item which has a name that is an enum constant.
 *
 * @param <E> the item type, which extends {@code Enum<?>} instead of {@code Enum<E>} to allow for items
 *        which can be named for any enum value
 */
@SuppressWarnings("unused")
public abstract class EnumNamedBuildItem<E extends Enum<?>> extends NamedBuildItem<E> {
    protected EnumNamedBuildItem() {
        super();
    }
}
