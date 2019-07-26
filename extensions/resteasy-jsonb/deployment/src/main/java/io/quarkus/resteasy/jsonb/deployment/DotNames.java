package io.quarkus.resteasy.jsonb.deployment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.json.bind.Jsonb;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbNillable;
import javax.json.bind.annotation.JsonbNumberFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.annotation.JsonbVisibility;
import javax.ws.rs.ext.ContextResolver;

import org.jboss.jandex.DotName;

public final class DotNames {

    private DotNames() {
    }

    public static final DotName OBJECT = DotName.createSimple(Object.class.getName());
    public static final DotName STRING = DotName.createSimple(String.class.getName());
    public static final DotName PRIMITIVE_BOOLEAN = DotName.createSimple(boolean.class.getName());
    public static final DotName PRIMITIVE_INT = DotName.createSimple(int.class.getName());
    public static final DotName PRIMITIVE_LONG = DotName.createSimple(long.class.getName());
    public static final DotName BOOLEAN = DotName.createSimple(Boolean.class.getName());
    public static final DotName INTEGER = DotName.createSimple(Integer.class.getName());
    public static final DotName LONG = DotName.createSimple(Long.class.getName());
    public static final DotName BIG_DECIMAL = DotName.createSimple(BigDecimal.class.getName());
    public static final DotName LOCAL_DATE_TIME = DotName.createSimple(LocalDateTime.class.getName());

    public static final DotName COLLECTION = DotName.createSimple(Collection.class.getName());
    public static final DotName LIST = DotName.createSimple(List.class.getName());
    public static final DotName SET = DotName.createSimple(Set.class.getName());

    public static final DotName OPTIONAL = DotName.createSimple(Optional.class.getName());

    public static final DotName MAP = DotName.createSimple(Map.class.getName());
    public static final DotName HASHMAP = DotName.createSimple(HashMap.class.getName());

    public static final DotName CONTEXT_RESOLVER = DotName.createSimple(ContextResolver.class.getName());

    public static final DotName JSONB = DotName.createSimple(Jsonb.class.getName());
    public static final DotName JSONB_TRANSIENT = DotName.createSimple(JsonbTransient.class.getName());
    public static final DotName JSONB_PROPERTY = DotName.createSimple(JsonbProperty.class.getName());
    public static final DotName JSONB_TYPE_SERIALIZER = DotName.createSimple(JsonbTypeSerializer.class.getName());
    public static final DotName JSONB_TYPE_ADAPTER = DotName.createSimple(JsonbTypeAdapter.class.getName());
    public static final DotName JSONB_VISIBILITY = DotName.createSimple(JsonbVisibility.class.getName());
    public static final DotName JSONB_NILLABLE = DotName.createSimple(JsonbNillable.class.getName());
    public static final DotName JSONB_PROPERTY_ORDER = DotName.createSimple(JsonbPropertyOrder.class.getName());
    public static final DotName JSONB_NUMBER_FORMAT = DotName.createSimple(JsonbNumberFormat.class.getName());
    public static final DotName JSONB_DATE_FORMAT = DotName.createSimple(JsonbDateFormat.class.getName());
}
