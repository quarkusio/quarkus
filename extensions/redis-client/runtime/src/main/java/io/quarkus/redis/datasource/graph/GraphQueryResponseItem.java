package io.quarkus.redis.datasource.graph;

import java.util.List;

public interface GraphQueryResponseItem {

    enum Kind {
        SCALAR,
        NODE,
        RELATION
    }

    Kind kind();

    String name();

    default ScalarItem asScalarItem() {
        if (this instanceof ScalarItem) {
            return (ScalarItem) this;
        }
        throw new ClassCastException(
                "Cannot cast " + this + " of kind " + kind() + " to a " + ScalarItem.class.getName());
    }

    default NodeItem asNodeItem() {
        if (this instanceof NodeItem) {
            return (NodeItem) this;
        }
        throw new ClassCastException(
                "Cannot cast " + this + " of kind " + kind() + " to a " + NodeItem.class.getName());
    }

    default RelationItem asRelationItem() {
        if (this instanceof RelationItem) {
            return (RelationItem) this;
        }
        throw new ClassCastException(
                "Cannot cast " + this + " of kind " + kind() + " to a " + RelationItem.class.getName());
    }

    interface ScalarItem extends GraphQueryResponseItem {

        boolean asBoolean();

        int asInteger();

        double asDouble();

        boolean isNull();

        String asString();

        @Override
        default Kind kind() {
            return Kind.SCALAR;
        }
    }

    interface NodeItem extends GraphQueryResponseItem {
        long id();

        List<String> labels();

        List<ScalarItem> properties();

        ScalarItem get(String property);

        @Override
        default Kind kind() {
            return Kind.NODE;
        }
    }

    interface RelationItem extends GraphQueryResponseItem {
        long id();

        String type();

        long source();

        long destination();

        List<ScalarItem> properties();

        ScalarItem get(String property);

        @Override
        default Kind kind() {
            return Kind.RELATION;
        }
    }

}
