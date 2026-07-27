package io.quarkus.core.deployment.action.impl;

import static java.lang.constant.ConstantDescs.CD_Class;
import static java.lang.constant.ConstantDescs.CD_Double;
import static java.lang.constant.ConstantDescs.CD_Float;
import static java.lang.constant.ConstantDescs.CD_Integer;
import static java.lang.constant.ConstantDescs.CD_List;
import static java.lang.constant.ConstantDescs.CD_Long;
import static java.lang.constant.ConstantDescs.CD_Map;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_Set;
import static java.lang.constant.ConstantDescs.CD_double;
import static java.lang.constant.ConstantDescs.CD_float;
import static java.lang.constant.ConstantDescs.CD_int;
import static java.lang.constant.ConstantDescs.CD_long;
import static java.lang.constant.ConstantDescs.CD_void;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

import io.smallrye.classfile.CodeBuilder;
import io.smallrye.classfile.TypeKind;
import io.smallrye.classfile.extras.constant.ExtraConstantDescs;
import io.smallrye.serial.SerialData;
import io.smallrye.serial.SerialField;
import io.smallrye.serial.Serialized;
import io.smallrye.serial.SerializedClass;
import io.smallrye.serial.SerializedNull;
import io.smallrye.serial.SerializedRecord;
import io.smallrye.serial.SerializedRecordClass;
import io.smallrye.serial.SerializedSerializable;
import io.smallrye.serial.StreamData;

/**
 * Walks a {@link Serialized} graph and produces {@link CaptureEmitter} instances
 * that emit the bytecode to reconstruct captured values at runtime.
 * <p>
 * All decomposition is performed by {@code smallrye-serial} via custom serializers;
 * this class only handles the bytecode emission from the resulting {@code Serialized} tree.
 */
final class SerializedCaptureEmitter {

    private static final ClassDesc CD_MAP_ENTRY = ClassDesc.of("java.util.Map$Entry");
    private static final ClassDesc CD_CONFIG_LOOKUP = ClassDesc.of("io.quarkus.core.runtime.ConfigLookup");

    /** CollSer tag: unmodifiable list. */
    private static final int TAG_LIST = 1;
    /** CollSer tag: unmodifiable set. */
    private static final int TAG_SET = 2;
    /** CollSer tag: unmodifiable map. */
    private static final int TAG_MAP = 3;

    /** Tracks dedup: Serialized identity → emitter (to avoid re-emitting shared nodes). */
    private final IdentityHashMap<Serialized, CaptureEmitter> dedupMap = new IdentityHashMap<>();

    SerializedCaptureEmitter() {
    }

    /**
     * Produce a {@link CaptureEmitter} for the given serialized node.
     *
     * @param serialized the serialized value (must not be {@code null})
     * @param targetType the target type descriptor (for boxing decisions)
     * @return the capture emitter
     */
    CaptureEmitter emitFor(Serialized serialized, ClassDesc targetType) {
        // dedup: return existing emitter for shared Serialized nodes
        CaptureEmitter existing = dedupMap.get(serialized);
        if (existing != null) {
            return existing;
        }

        CaptureEmitter result;
        boolean doDedup = false;
        if (serialized instanceof SerializedConstant sc) {
            result = emitConstant(sc, targetType);
        } else if (serialized instanceof SerializedConfigMapping cm) {
            result = emitConfigMapping(cm);
            doDedup = true;
        } else if (serialized instanceof SerializedNull) {
            result = CodeBuilder::aconst_null;
        } else if (serialized instanceof SerializedRecord sr) {
            result = emitRecord(sr);
            doDedup = true;
        } else if (serialized instanceof SerializedClass sc) {
            result = emitClassConstant(sc);
        } else if (serialized instanceof SerializedSerializable ss) {
            result = emitSerializable(ss);
            doDedup = true;
        } else {
            throw new LambdaTransliterator.TransliterationException(
                    "Unsupported serialized type: " + serialized.getClass().getName());
        }

        // wrap non-trivial emitters in dedup (constants are cheap to re-emit)
        if (doDedup) {
            DedupEmitter dedup = new DedupEmitter(result);
            dedupMap.put(serialized, dedup);
            return dedup;
        }
        dedupMap.put(serialized, result);
        return result;
    }

    // ── Constants ──

    /**
     * Emit a constant value, with boxing if the target type is a reference type
     * but the constant pushes a primitive.
     */
    private static CaptureEmitter emitConstant(SerializedConstant sc, ClassDesc targetType) {
        ConstantDesc cd = sc.constantDesc();
        if (targetType.isPrimitive()) {
            // target is primitive — load as primitive constant
            // Boolean/Byte/Short/Character are already stored as int by ConstantDescSerializer
            return code -> code.loadConstant(cd);
        }
        // target is reference — may need boxing
        ClassDesc boxType = sc.boxType();
        if (boxType == null) {
            // check for Integer/Long/Float/Double (which push primitives via ldc)
            boxType = inferBoxType(cd);
        }
        if (boxType != null) {
            ClassDesc bt = boxType;
            ClassDesc primType = inferPrimType(cd);
            return code -> {
                code.loadConstant(cd);
                code.invokestatic(bt, "valueOf", MethodTypeDesc.of(bt, primType));
            };
        }
        return code -> code.loadConstant(cd);
    }

    /**
     * Infer the box type for a ConstantDesc that pushes a primitive via ldc.
     * Returns null if the constant pushes a reference (no boxing needed).
     */
    private static ClassDesc inferBoxType(ConstantDesc cd) {
        if (cd instanceof Integer) {
            return CD_Integer;
        }
        if (cd instanceof Long) {
            return CD_Long;
        }
        if (cd instanceof Float) {
            return CD_Float;
        }
        if (cd instanceof Double) {
            return CD_Double;
        }
        return null;
    }

    /**
     * Infer the primitive type for a ConstantDesc that pushes a primitive.
     * For Boolean/Byte/Short/Character (stored as Integer by ConstantDescSerializer),
     * the primitive type is always int.
     */
    private static ClassDesc inferPrimType(ConstantDesc cd) {
        if (cd instanceof Integer) {
            return CD_int;
        }
        if (cd instanceof Long) {
            return CD_long;
        }
        if (cd instanceof Float) {
            return CD_float;
        }
        if (cd instanceof Double) {
            return CD_double;
        }
        throw new IllegalArgumentException("Unexpected primitive constant: " + cd.getClass());
    }

    // ── Config mappings ──

    /**
     * Emit a config mapping lookup via ConfigLookup.getConfigMapping(type).
     */
    private static CaptureEmitter emitConfigMapping(SerializedConfigMapping cm) {
        ClassDesc configType = cm.configInterface();
        return code -> {
            code.loadConstant(configType);
            code.invokestatic(CD_CONFIG_LOOKUP, "getConfigMapping",
                    MethodTypeDesc.of(CD_Object, CD_Class));
            code.checkcast(configType);
        };
    }

    // ── Class constants ──

    /**
     * Emit a Class constant from a SerializedClass node.
     */
    private static CaptureEmitter emitClassConstant(SerializedClass sc) {
        ClassDesc classDesc = sc.descriptor();
        return code -> code.loadConstant(classDesc);
    }

    // ── Records ──

    /**
     * Emit record reconstruction: {@code new RecordType(component1, component2, ...)}.
     * Serialized fields are name-sorted (per Java serialization spec), but the canonical
     * constructor takes parameters in declaration order. We load the class to get
     * RecordComponent[] and reorder accordingly.
     */
    private CaptureEmitter emitRecord(SerializedRecord sr) {
        SerializedRecordClass rc = sr.recordClass();
        ClassDesc recordDesc = rc.descriptor();
        SerialData fieldData = sr.fieldData();

        // load the class to get declaration-order components
        Class<?> recordClass;
        try {
            recordClass = Class.forName(rc.name(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new LambdaTransliterator.TransliterationException(
                    "Record class not found: " + rc.name(), e);
        }
        RecordComponent[] components = recordClass.getRecordComponents();

        // resolve emitters in declaration order, reading values by name from serialized data
        CaptureEmitter[] componentEmitters = new CaptureEmitter[components.length];
        ClassDesc[] componentTypes = new ClassDesc[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent comp = components[i];
            String name = comp.getName();
            Class<?> type = comp.getType();
            componentTypes[i] = type.describeConstable().orElseThrow();
            if (type.isPrimitive()) {
                SerialField field = rc.streamField(name);
                componentEmitters[i] = emitPrimitiveField(fieldData, field);
            } else {
                Serialized fieldValue = fieldData.getObject(name);
                componentEmitters[i] = emitFor(fieldValue, componentTypes[i]);
            }
        }

        MethodTypeDesc ctorType = MethodTypeDesc.of(CD_void, componentTypes);
        return code -> {
            code.new_(recordDesc);
            code.dup();
            for (CaptureEmitter emitter : componentEmitters) {
                emitter.emit(code);
            }
            code.invokespecial(recordDesc, ExtraConstantDescs.INIT_NAME, ctorType);
        };
    }

    /**
     * Emit a primitive field value from SerialData as a constant.
     */
    private static CaptureEmitter emitPrimitiveField(SerialData data, SerialField field) {
        return switch (field.typeCode()) {
            case 'Z' -> code -> code.loadConstant(data.getBoolean(field.name()) ? 1 : 0);
            case 'B' -> code -> code.loadConstant(data.getByte(field.name()));
            case 'C' -> code -> code.loadConstant(data.getChar(field.name()));
            case 'S' -> code -> code.loadConstant(data.getShort(field.name()));
            case 'I' -> code -> code.loadConstant(data.getInt(field.name()));
            case 'J' -> code -> code.loadConstant(data.getLong(field.name()));
            case 'F' -> code -> code.loadConstant(data.getFloat(field.name()));
            case 'D' -> code -> code.loadConstant(data.getDouble(field.name()));
            default -> throw new LambdaTransliterator.TransliterationException(
                    "Unknown primitive type code: " + field.typeCode());
        };
    }

    // ── Serializable (collections, maps, wrappers) ──

    /**
     * Emit a serializable object — dispatches to collection/map handlers based on
     * the serialized class name.
     */
    private CaptureEmitter emitSerializable(SerializedSerializable ss) {
        String className = ss.serializedClass().name();
        return switch (className) {
            case "java.util.CollSer" -> emitCollSer(ss);
            case "java.util.Collections$EmptyList" -> emitFactoryCall(CD_List, "of", new CaptureEmitter[0]);
            case "java.util.Collections$EmptySet" -> emitFactoryCall(CD_Set, "of", new CaptureEmitter[0]);
            case "java.util.Collections$EmptyMap" -> emitMapOf(new CaptureEmitter[0], new CaptureEmitter[0]);
            case "java.util.Collections$SingletonList" -> emitSingletonCollection(ss, CD_List);
            case "java.util.Collections$SingletonSet" -> emitSingletonCollection(ss, CD_Set);
            case "java.util.Collections$SingletonMap" -> emitSingletonMap(ss);
            case "java.util.Collections$UnmodifiableList",
                    "java.util.Collections$UnmodifiableRandomAccessList" ->
                emitUnmodifiableCollection(ss, CD_List);
            case "java.util.Collections$UnmodifiableSet",
                    "java.util.Collections$UnmodifiableSortedSet",
                    "java.util.Collections$UnmodifiableNavigableSet" ->
                emitUnmodifiableCollection(ss, CD_Set);
            case "java.util.Collections$UnmodifiableMap",
                    "java.util.Collections$UnmodifiableSortedMap",
                    "java.util.Collections$UnmodifiableNavigableMap" ->
                emitUnmodifiableMap(ss);
            // mutable collection types — reject when captured directly
            case "java.util.ArrayList", "java.util.LinkedList",
                    "java.util.HashSet", "java.util.TreeSet",
                    "java.util.HashMap", "java.util.TreeMap",
                    "java.util.LinkedHashSet", "java.util.LinkedHashMap" ->
                throw new LambdaTransliterator.TransliterationException(
                        "Unsupported collection/map type: " + className
                                + " — only unmodifiable List, Set, and Map types created via"
                                + " List.of(), Set.of(), Map.of(), Collections.unmodifiableList/Set/Map(),"
                                + " Collections.singletonList/Set/Map(), or Collections.emptyList/Set/Map()"
                                + " are supported as captured values");
            default -> throw new LambdaTransliterator.TransliterationException(
                    "Unsupported serializable type in capture: " + className);
        };
    }

    /**
     * Handle JDK immutable collections via CollSer.
     * The tag field determines the collection type (1=list, 2=set, 3=map, 4=nullable list).
     * Elements are in the stream data.
     */
    private CaptureEmitter emitCollSer(SerializedSerializable ss) {
        SerialData data = ss.dataFor("java.util.CollSer");
        int tag = data.getInt("tag") & 0xFF;
        // elements are in the stream data as objects
        Serialized[] elements = extractStreamObjects(data);
        return switch (tag) {
            case TAG_LIST, 4 -> {
                CaptureEmitter[] emitters = resolveElements(elements);
                yield emitFactoryCall(CD_List, "of", emitters);
            }
            case TAG_SET -> {
                CaptureEmitter[] emitters = resolveElements(elements);
                yield emitFactoryCall(CD_Set, "of", emitters);
            }
            case TAG_MAP -> {
                // map elements are interleaved: key, value, key, value, ...
                int entryCount = elements.length / 2;
                CaptureEmitter[] keys = new CaptureEmitter[entryCount];
                CaptureEmitter[] values = new CaptureEmitter[entryCount];
                for (int i = 0; i < entryCount; i++) {
                    keys[i] = emitFor(elements[i * 2], CD_Object);
                    values[i] = emitFor(elements[i * 2 + 1], CD_Object);
                }
                yield emitMapOf(keys, values);
            }
            default -> throw new LambdaTransliterator.TransliterationException(
                    "Unsupported CollSer tag " + tag);
        };
    }

    /**
     * Handle Collections.singletonList / Collections.singleton.
     */
    private CaptureEmitter emitSingletonCollection(SerializedSerializable ss, ClassDesc collType) {
        // singleton collections have a single "element" field
        SerialData data = ss.data().get(0);
        Serialized element = data.getObject("element");
        CaptureEmitter elementEmitter = emitFor(element, CD_Object);
        return emitFactoryCall(collType, "of", new CaptureEmitter[] { elementEmitter });
    }

    /**
     * Handle Collections.singletonMap.
     */
    private CaptureEmitter emitSingletonMap(SerializedSerializable ss) {
        SerialData data = ss.data().get(0);
        Serialized key = data.getObject("k");
        Serialized value = data.getObject("v");
        return emitMapOf(
                new CaptureEmitter[] { emitFor(key, CD_Object) },
                new CaptureEmitter[] { emitFor(value, CD_Object) });
    }

    /**
     * Handle Collections.unmodifiableList/Set.
     * The backing collection is in the "c" field at the UnmodifiableCollection level.
     */
    private CaptureEmitter emitUnmodifiableCollection(SerializedSerializable ss, ClassDesc collType) {
        SerialData ucData = ss.dataFor("java.util.Collections$UnmodifiableCollection");
        Serialized backing = ucData.getObject("c");
        if (backing instanceof SerializedSerializable backingSs) {
            return emitBackingCollection(backingSs, collType);
        }
        // backing might be a CollSer (immutable factory type) — recurse normally
        return emitFor(backing, collType);
    }

    /**
     * Handle Collections.unmodifiableMap.
     * The backing map is in the "m" field at the UnmodifiableMap level.
     */
    private CaptureEmitter emitUnmodifiableMap(SerializedSerializable ss) {
        SerialData umData = ss.dataFor("java.util.Collections$UnmodifiableMap");
        Serialized backing = umData.getObject("m");
        if (backing instanceof SerializedSerializable backingSs) {
            return emitBackingMap(backingSs);
        }
        return emitFor(backing, CD_Map);
    }

    // ── Backing collections (for unmodifiable wrappers) ──

    /**
     * Emit a backing collection (ArrayList, LinkedList, HashSet) as List.of/Set.of.
     * Elements are in the stream data written by the collection's writeObject method.
     */
    private CaptureEmitter emitBackingCollection(SerializedSerializable ss, ClassDesc collType) {
        // ArrayList/LinkedList/HashSet write elements to stream data via writeObject
        SerialData data = ss.data().get(ss.data().size() - 1);
        Serialized[] elements = extractStreamObjects(data);
        CaptureEmitter[] emitters = resolveElements(elements);
        if (collType.descriptorString().equals(CD_List.descriptorString())) {
            return emitFactoryCall(CD_List, "of", emitters);
        } else {
            return emitFactoryCall(CD_Set, "of", emitters);
        }
    }

    /**
     * Emit a backing map (HashMap) as Map.of.
     * Entries are written as key, value, key, value, ... in stream data.
     */
    private CaptureEmitter emitBackingMap(SerializedSerializable ss) {
        SerialData data = ss.data().get(ss.data().size() - 1);
        Serialized[] elements = extractStreamObjects(data);
        int entryCount = elements.length / 2;
        CaptureEmitter[] keys = new CaptureEmitter[entryCount];
        CaptureEmitter[] values = new CaptureEmitter[entryCount];
        for (int i = 0; i < entryCount; i++) {
            keys[i] = emitFor(elements[i * 2], CD_Object);
            values[i] = emitFor(elements[i * 2 + 1], CD_Object);
        }
        return emitMapOf(keys, values);
    }

    // ── Helpers ──

    /**
     * Extract all objects from the stream data of a SerialData.
     */
    private static Serialized[] extractStreamObjects(SerialData data) {
        List<StreamData> streamDataList = data.streamData();
        if (streamDataList.isEmpty()) {
            return new Serialized[0];
        }
        // collect all objects from all stream data blocks
        int total = 0;
        for (StreamData sd : streamDataList) {
            if (sd instanceof StreamData.OfObjects oo) {
                total += oo.size();
            }
        }
        Serialized[] result = new Serialized[total];
        int idx = 0;
        for (StreamData sd : streamDataList) {
            if (sd instanceof StreamData.OfObjects oo) {
                for (int i = 0; i < oo.size(); i++) {
                    result[idx++] = oo.getObject(i);
                }
            }
        }
        return result;
    }

    /**
     * Resolve an array of Serialized elements to emitters (for collection elements).
     */
    private CaptureEmitter[] resolveElements(Serialized[] elements) {
        CaptureEmitter[] emitters = new CaptureEmitter[elements.length];
        for (int i = 0; i < elements.length; i++) {
            emitters[i] = emitFor(elements[i], CD_Object);
        }
        return emitters;
    }

    /**
     * Emit a static factory call (List.of, Set.of) with the given elements.
     * Uses specific overloads for ≤10 elements, varargs for >10.
     */
    private static CaptureEmitter emitFactoryCall(ClassDesc owner, String method, CaptureEmitter[] elements) {
        int count = elements.length;
        if (count <= 10) {
            ClassDesc[] params = new ClassDesc[count];
            Arrays.fill(params, CD_Object);
            MethodTypeDesc mtd = MethodTypeDesc.of(owner, params);
            return code -> {
                for (CaptureEmitter e : elements) {
                    e.emit(code);
                }
                code.invokestatic(owner, method, mtd, true);
            };
        } else {
            return code -> {
                code.loadConstant(count);
                code.anewarray(CD_Object);
                for (int i = 0; i < count; i++) {
                    code.dup();
                    code.loadConstant(i);
                    elements[i].emit(code);
                    code.aastore();
                }
                code.invokestatic(owner, method,
                        MethodTypeDesc.of(owner, CD_Object.arrayType()), true);
            };
        }
    }

    /**
     * Emit a Map.of or Map.ofEntries call.
     */
    private static CaptureEmitter emitMapOf(CaptureEmitter[] keys, CaptureEmitter[] values) {
        int count = keys.length;
        if (count <= 10) {
            return code -> {
                ClassDesc[] params = new ClassDesc[count * 2];
                for (int i = 0; i < count; i++) {
                    keys[i].emit(code);
                    values[i].emit(code);
                    params[i << 1] = CD_Object;
                    params[(i << 1) + 1] = CD_Object;
                }
                code.invokestatic(CD_Map, "of",
                        MethodTypeDesc.of(CD_Map, params), true);
            };
        } else {
            return code -> {
                code.loadConstant(count);
                code.anewarray(CD_MAP_ENTRY);
                for (int i = 0; i < count; i++) {
                    code.dup();
                    code.loadConstant(i);
                    keys[i].emit(code);
                    values[i].emit(code);
                    code.invokestatic(CD_Map, "entry",
                            MethodTypeDesc.of(CD_MAP_ENTRY, CD_Object, CD_Object), true);
                    code.aastore();
                }
                code.invokestatic(CD_Map, "ofEntries",
                        MethodTypeDesc.of(CD_Map, CD_MAP_ENTRY.arrayType()), true);
            };
        }
    }

    /**
     * Reuse the existing DedupEmitter from LambdaTransliterator.
     * Emits the delegate once and stores in a local; subsequent calls load from local.
     */
    private static final class DedupEmitter implements CaptureEmitter {
        private final CaptureEmitter delegate;
        private int slot = -1;

        DedupEmitter(CaptureEmitter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void emit(CodeBuilder code) {
            if (slot == -1) {
                delegate.emit(code);
                slot = code.allocateLocal(TypeKind.REFERENCE);
                code.dup();
                code.astore(slot);
            } else {
                code.aload(slot);
            }
        }
    }
}
