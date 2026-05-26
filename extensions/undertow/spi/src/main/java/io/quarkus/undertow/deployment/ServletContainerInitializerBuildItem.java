package io.quarkus.undertow.deployment;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.builder.item.MultiBuildItem;

public final class ServletContainerInitializerBuildItem extends MultiBuildItem
        implements Comparable<ServletContainerInitializerBuildItem> {

    final String sciClass;
    final Set<String> handlesTypes;

    public ServletContainerInitializerBuildItem(String sciClass, Set<String> handlesTypes) {
        this.sciClass = sciClass;
        this.handlesTypes = new TreeSet<>(handlesTypes);
    }

    public String getSciClass() {
        return sciClass;
    }

    public Set<String> getHandlesTypes() {
        return handlesTypes;
    }

    @Override
    public int compareTo(ServletContainerInitializerBuildItem o) {
        int result = sciClass.compareTo(o.sciClass);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(handlesTypes.size(), o.handlesTypes.size());
        if (result != 0) {
            return result;
        }
        Iterator<String> thisIterator = handlesTypes.iterator();
        Iterator<String> otherIterator = o.handlesTypes.iterator();
        while (thisIterator.hasNext()) {
            result = thisIterator.next().compareTo(otherIterator.next());
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

}
