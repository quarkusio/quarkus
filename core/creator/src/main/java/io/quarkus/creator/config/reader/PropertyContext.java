package io.quarkus.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertyContext {

    final PropertyContext parent;
    final String mappedName;
    final int mappedNameElements;
    final String[] nameEls;
    final PropertiesHandler<?> handler;
    Object o;

    PropertyLine nestedProperty;
    int nameElement;

    PropertyContext(PropertyContext parent, String mappedName, int mappedNameElements, String[] nameEls,
            PropertiesHandler<?> handler) {
        this.parent = parent;
        this.mappedName = mappedName;
        this.mappedNameElements = mappedNameElements;
        this.nameEls = nameEls;
        this.handler = handler;
    }

    public String getRelativeName() {
        return nestedProperty.getRelativeName(nameElement);
    }

    public String getValue() {
        return nestedProperty.getValue();
    }

    public PropertyLine getLine() {
        return nestedProperty;
    }
}
