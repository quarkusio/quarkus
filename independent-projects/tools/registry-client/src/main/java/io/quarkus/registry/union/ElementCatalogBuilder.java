package io.quarkus.registry.union;

import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ElementCatalogBuilder<M> {

    public static <T> ElementCatalogBuilder<T> newInstance() {
        return new ElementCatalogBuilder<T>();
    }

    public static class ElementBuilder<T> extends BuildCallback<Member<T>> {

        private final Object key;
        private final Object version;
        private final List<BuildCallback<Element<T>>> callbacks = new ArrayList<>();
        private final List<Member<T>> members = new ArrayList<>();

        private ElementBuilder(Object key, Object version) {
            this.key = Objects.requireNonNull(key);
            this.version = Objects.requireNonNull(version);
        }

        private ElementBuilder<T> addCallback(MemberBuilder<T> callback) {
            callbacks.add(callback);
            callback.callbacks.add(this);
            return this;
        }

        private Element<T> build() {
            final Element<T> e = new Element<T>() {
                @Override
                public Object key() {
                    return key;
                }

                @Override
                public Collection<Member<T>> members() {
                    return members;
                }

                @Override
                public String toString() {
                    return key.toString() + "#" + version;
                }
            };
            callbacks.forEach(c -> c.created(e));
            return e;
        }

        @Override
        protected void created(Member<T> t) {
            members.add(t);
        }
    }

    public static class MemberBuilder<T> extends BuildCallback<Element<T>> {
        private final Object key;
        private final Object version;
        private final ElementCatalogBuilder<T> catalogBuilder;
        private final T instance;
        private Union<T> initialUnion;
        private List<UnionVersion> unionVersions = new ArrayList<>();
        private final List<BuildCallback<Member<T>>> callbacks = new ArrayList<>();
        private final Map<Object, Element<T>> elements = new HashMap<>();

        private MemberBuilder(Object key, Object version, ElementCatalogBuilder<T> catalogBuilder, T instance) {
            this.key = Objects.requireNonNull(key);
            this.version = Objects.requireNonNull(version);
            this.catalogBuilder = catalogBuilder;
            this.instance = instance;
        }

        public ElementBuilder<T> addElement(Object elementKey) {
            return catalogBuilder.getOrCreateElement(elementKey, version).addCallback(this);
        }

        MemberBuilder<T> addUnion(UnionBuilder<T> union) {
            callbacks.add(union);
            unionVersions.add(union.version);
            return this;
        }

        @Override
        protected void created(Element<T> t) {
            elements.put(t.key(), t);
        }

        public Member<T> build() {
            final Member<T> m = new Member<T>() {

                @Override
                public Object key() {
                    return key;
                }

                @Override
                public Object version() {
                    return version;
                }

                @Override
                public Union<T> initialUnion() {
                    return initialUnion;
                }

                @Override
                public Collection<Element<T>> elements() {
                    return elements.values();
                }

                @Override
                public Collection<Object> elementKeys() {
                    return elements.keySet();
                }

                @Override
                public Element<T> get(Object elementKey) {
                    return elements.get(elementKey);
                }

                @Override
                public String toString() {
                    return key.toString() + "#" + version + elements.values();
                }

                @Override
                public boolean containsAll(Collection<Object> elementKeys) {
                    return elements.keySet().containsAll(elementKeys);
                }

                @Override
                public Collection<UnionVersion> unions() {
                    return unionVersions;
                }

                @Override
                public boolean isEmpty() {
                    return elements.isEmpty();
                }

                @Override
                public T getInstance() {
                    return instance;
                }
            };
            callbacks.forEach(c -> c.created(m));
            return m;
        }
    }

    public static class UnionBuilder<T> extends BuildCallback<Member<T>> {

        private final UnionVersion version;
        private final ElementCatalogBuilder<T> catalogBuilder;
        private final List<MemberBuilder<T>> memberBuilders = new ArrayList<>();
        private final Map<Object, Member<T>> members = new HashMap<>();

        private UnionBuilder(UnionVersion version, ElementCatalogBuilder<T> catalogBuilder) {
            this.version = Objects.requireNonNull(version);
            this.catalogBuilder = catalogBuilder;
        }

        public UnionVersion version() {
            return version;
        }

        public MemberBuilder<T> getOrCreateMember(Object memberKey, Object memberVersion) {
            return getOrCreateMember(memberKey, memberVersion, null);
        }

        public MemberBuilder<T> getOrCreateMember(Object memberKey, Object memberVersion, T instance) {
            final MemberBuilder<T> mb = catalogBuilder.getOrCreateMember(memberKey, memberVersion, instance);
            memberBuilders.add(mb);
            return mb.addUnion(this);
        }

        @Override
        protected void created(Member<T> t) {
            members.put(t.key(), t);
        }

        public Union<T> build() {
            final Union<T> u = new Union<T>() {

                @Override
                public UnionVersion version() {
                    return version;
                }

                @Override
                public Collection<Member<T>> members() {
                    return members.values();
                }

                @Override
                public Member<T> member(Object memberKey) {
                    return members.get(memberKey);
                }

                @Override
                public String toString() {
                    return version.toString() + members;
                }
            };
            for (MemberBuilder<T> mb : memberBuilders) {
                mb.initialUnion = u;
            }
            return u;
        }
    }

    private static abstract class BuildCallback<T> {

        protected abstract void created(T t);
    }

    static class IntVersion implements UnionVersion {

        static UnionVersion get(Integer i) {
            return new IntVersion(i);
        }

        private final Integer version;

        public IntVersion(int version) {
            this.version = version;
        }

        @Override
        public int compareTo(UnionVersion o) {
            if (o instanceof IntVersion) {
                return version.compareTo(((IntVersion) o).version);
            }
            throw new IllegalArgumentException(o + " is not an instance of " + IntVersion.class.getName());
        }

        @Override
        public String toString() {
            return version.toString();
        }
    }

    private final Map<Object, ElementBuilder<M>> elements = new HashMap<>();
    private final Map<Object, MemberBuilder<M>> members = new HashMap<>();
    private final Map<UnionVersion, UnionBuilder<M>> unions = new HashMap<>();

    private ElementBuilder<M> getOrCreateElement(Object elementKey, Object elementVersion) {
        return elements.computeIfAbsent(elementKey, k -> new ElementBuilder<M>(k, elementVersion));
    }

    private MemberBuilder<M> getOrCreateMember(Object key, Object version, M instance) {
        return members.computeIfAbsent(key + ":" + version, k -> new MemberBuilder<M>(key, version, this, instance));
    }

    public UnionBuilder<M> getOrCreateUnion(int version) {
        return getOrCreateUnion(IntVersion.get(version));
    }

    public UnionBuilder<M> getOrCreateUnion(UnionVersion version) {
        return unions.computeIfAbsent(version, v -> new UnionBuilder<M>(version, this));
    }

    public ElementCatalog<M> build() {

        final Map<Object, Element<M>> map = new HashMap<>(elements.size());
        for (ElementBuilder<M> eb : elements.values()) {
            final Element<M> e = eb.build();
            map.put(e.key(), e);
        }
        for (MemberBuilder<M> m : members.values()) {
            m.build();
        }
        for (UnionBuilder<M> u : unions.values()) {
            u.build();
        }

        final ElementCatalog<M> catalog = new ElementCatalog<M>() {

            @Override
            public Collection<Element<M>> elements() {
                return map.values();
            }

            @Override
            public Collection<Object> elementKeys() {
                return elements.keySet();
            }

            @Override
            public Element<M> get(Object elementKey) {
                return map.get(elementKey);
            }

            @Override
            public String toString() {
                return elements.toString();
            }

            @Override
            public boolean isEmpty() {
                return map.isEmpty();
            }
        };

        return catalog;

    }

    public static <T> void dump(PrintStream ps, ElementCatalog<T> catalog) {
        ps.println("Element Catalog:");
        final Map<UnionVersion, Map<Object, Member<T>>> unions = new TreeMap<>();
        for (Element<T> e : catalog.elements()) {
            for (Member<T> m : e.members()) {
                for (UnionVersion uv : m.unions()) {
                    unions.computeIfAbsent(uv, v -> new HashMap<>()).put(m.key(), m);
                }
            }
        }
        for (Map.Entry<UnionVersion, Map<Object, Member<T>>> entry : unions.entrySet()) {
            System.out.println("Union " + entry.getKey());
            for (Member<T> m : entry.getValue().values()) {
                System.out.println("  Member " + m.key() + ":" + m.version());
                for (Object e : m.elementKeys()) {
                    System.out.println("    Element " + e);
                }
            }
        }
    }

    public static <T> List<T> getMembersForElements(ElementCatalog<T> elementCatalog, Collection<String> elementKeys) {
        final Map<UnionVersion, Map<Object, Member<T>>> unionVersions = new TreeMap<>(UnionVersion::compareTo);
        for (Object elementKey : elementKeys) {
            final Element<T> e = elementCatalog.get(elementKey);
            if (e == null) {
                throw new RuntimeException(
                        "Element " + elementKey + " not found in the catalog " + elementCatalog.elementKeys());
            }
            for (Member<T> m : e.members()) {
                for (UnionVersion uv : m.unions()) {
                    unionVersions.computeIfAbsent(uv, v -> new IdentityHashMap<>()).put(m, m);
                }
            }
        }

        final Set<Object> memberElementKeys = new HashSet<>();
        for (Map<Object, Member<T>> members : unionVersions.values()) {
            memberElementKeys.clear();
            for (Member<T> m : members.values()) {
                memberElementKeys.addAll(m.elementKeys());
            }
            if (memberElementKeys.containsAll(elementKeys)) {
                return members.values().stream().map(Member::getInstance).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    public static void addUnionMember(final UnionBuilder<ExtensionCatalog> union, final ExtensionCatalog ec) {
        final MemberBuilder<ExtensionCatalog> builder = union.getOrCreateMember(
                ec.getId(), ec.getBom().getVersion(), ec);
        ec.getExtensions()
                .forEach(e -> builder
                        .addElement(e.getArtifact().getGroupId() + ":" + e.getArtifact().getArtifactId()));
    }

    public static void setElementCatalog(ExtensionCatalog extCatalog, ElementCatalog<?> elemCatalog) {
        if (!elemCatalog.isEmpty()) {
            // TODO it's a hack to attach the "element" catalog to the extension catalog
            Map<String, Object> metadata = extCatalog.getMetadata();
            if (metadata.isEmpty()) {
                metadata = new HashMap<>(1);
                ((JsonExtensionCatalog) extCatalog).setMetadata(metadata);
            }
            metadata.put("element-catalog", elemCatalog);
        }
    }

    public static <T> ElementCatalog<T> getElementCatalog(ExtensionCatalog extCatalog, Class<T> t) {
        return (ElementCatalog<T>) extCatalog.getMetadata().get("element-catalog");
    }
}
