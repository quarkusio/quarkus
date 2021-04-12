package io.quarkus.registry.union;

import io.quarkus.maven.ArtifactCoords;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public class ElementCatalogBuilder {

    public static ElementCatalogBuilder newInstance() {
        return new ElementCatalogBuilder();
    }

    public class ElementBuilder extends BuildCallback<Member> {

        private final Object key;
        private final Object version;
        private final List<BuildCallback<Element>> callbacks = new ArrayList<>(4);
        private final List<Member> members = new ArrayList<>();

        private ElementBuilder(Object key, Object version) {
            this.key = Objects.requireNonNull(key);
            this.version = Objects.requireNonNull(version);
        }

        private ElementBuilder addCallback(MemberBuilder callback) {
            callbacks.add(callback);
            callback.callbacks.add(this);
            return this;
        }

        private Element build() {
            final Element e = new Element() {
                @Override
                public Object key() {
                    return key;
                }

                @Override
                public Collection<Member> members() {
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
        protected void created(Member t) {
            members.add(t);
        }
    }

    public class MemberBuilder extends BuildCallback<Element> {
        private final Object key;
        private final Object version;
        private Union initialUnion;
        private List<UnionVersion> unionVersions = new ArrayList<>();
        private final List<BuildCallback<Member>> callbacks = new ArrayList<>();
        private final Map<Object, Element> elements = new HashMap<>();

        private MemberBuilder(Object key, Object version) {
            this.key = Objects.requireNonNull(key);
            this.version = Objects.requireNonNull(version);
        }

        public ElementBuilder addElement(Object elementKey) {
            return getOrCreateElement(elementKey, version).addCallback(this);
        }

        MemberBuilder addUnion(UnionBuilder union) {
            callbacks.add(union);
            unionVersions.add(union.version);
            return this;
        }

        @Override
        protected void created(Element t) {
            elements.put(t.key(), t);
        }

        public Member build() {
            final Member m = new Member() {

                @Override
                public Object key() {
                    return key;
                }

                @Override
                public Object version() {
                    return version;
                }

                @Override
                public Union initialUnion() {
                    return initialUnion;
                }

                @Override
                public Collection<Element> elements() {
                    return elements.values();
                }

                @Override
                public Collection<Object> elementKeys() {
                    return elements.keySet();
                }

                @Override
                public Element get(Object elementKey) {
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
            };
            callbacks.forEach(c -> c.created(m));
            return m;
        }
    }

    public class UnionBuilder extends BuildCallback<Member> {

        private final UnionVersion version;
        private final List<MemberBuilder> memberBuilders = new ArrayList<>();
        private final Map<Object, Member> members = new HashMap<>();

        private UnionBuilder(UnionVersion version) {
            this.version = Objects.requireNonNull(version);
        }

        public MemberBuilder getOrCreateMember(Object memberKey, Object memberVersion) {
            final MemberBuilder mb = ElementCatalogBuilder.this.getOrCreateMember(memberKey, memberVersion);
            memberBuilders.add(mb);
            return mb.addUnion(this);
        }

        @Override
        protected void created(Member t) {
            members.put(t.key(), t);
        }

        public Union build() {
            final Union u = new Union() {

                @Override
                public UnionVersion verion() {
                    return version;
                }

                @Override
                public Collection<Member> members() {
                    return members.values();
                }

                @Override
                public Member member(Object memberKey) {
                    return members.get(memberKey);
                }

                @Override
                public String toString() {
                    return version.toString() + members;
                }
            };
            for (MemberBuilder mb : memberBuilders) {
                mb.initialUnion = u;
            }
            return u;
        }
    }

    private abstract class BuildCallback<T> {

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

    private final Map<Object, ElementBuilder> elements = new HashMap<>();
    private final Map<Object, MemberBuilder> members = new HashMap<>();
    private final Map<UnionVersion, UnionBuilder> unions = new HashMap<>();

    private ElementBuilder getOrCreateElement(Object elementKey, Object elementVersion) {
        return elements.computeIfAbsent(elementKey, k -> new ElementBuilder(k, elementVersion));
    }

    private MemberBuilder getOrCreateMember(Object key, Object version) {
        return members.computeIfAbsent(key + ":" + version, k -> new MemberBuilder(key, version));
    }

    public UnionBuilder getOrCreateUnion(int version) {
        return getOrCreateUnion(IntVersion.get(version));
    }

    public UnionBuilder getOrCreateUnion(UnionVersion version) {
        return unions.computeIfAbsent(version, v -> new UnionBuilder(version));
    }

    public ElementCatalog build() {

        final Map<Object, Element> map = new HashMap<>(elements.size());
        for (ElementBuilder eb : elements.values()) {
            final Element e = eb.build();
            map.put(e.key(), e);
        }
        for (MemberBuilder m : members.values()) {
            m.build();
        }
        for (UnionBuilder u : unions.values()) {
            u.build();
        }

        final ElementCatalog catalog = new ElementCatalog() {

            @Override
            public Collection<Element> elements() {
                return map.values();
            }

            @Override
            public Collection<Object> elementKeys() {
                return elements.keySet();
            }

            @Override
            public Element get(Object elementKey) {
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

    public static void dump(PrintStream ps, ElementCatalog catalog) {
        ps.println("Element Catalog:");
        final Map<UnionVersion, Map<Object, Member>> unions = new TreeMap<>();
        for (Element e : catalog.elements()) {
            for (Member m : e.members()) {
                for (UnionVersion uv : m.unions()) {
                    unions.computeIfAbsent(uv, v -> new HashMap<>()).put(m.key(), m);
                }
            }
        }
        for (Map.Entry<UnionVersion, Map<Object, Member>> entry : unions.entrySet()) {
            System.out.println("Union " + entry.getKey());
            for (Member m : entry.getValue().values()) {
                System.out.println("  Member " + m.key() + ":" + m.version());
                for (Object e : m.elementKeys()) {
                    System.out.println("    Element " + e);
                }
            }
        }
    }

    public static List<ArtifactCoords> getBoms(ElementCatalog elementCatalog, Collection<String> elementKeys) {

        final Comparator<UnionVersion> comparator = UnionVersion::compareTo;
        final Map<UnionVersion, Map<ArtifactCoords, Member>> unionVersions = new TreeMap<>(comparator.reversed());
        for (Object elementKey : elementKeys) {
            final Element e = elementCatalog.get(elementKey);
            if (e == null) {
                throw new RuntimeException(
                        "Element " + elementKey + " not found in the catalog " + elementCatalog.elementKeys());
            }
            for (Member m : e.members()) {
                for (UnionVersion uv : m.unions()) {
                    unionVersions.computeIfAbsent(uv, v -> new HashMap<>())
                            .put(ArtifactCoords.fromString(m.key() + "::pom:" + m.version()), m);
                }
            }
        }

        for (Map<ArtifactCoords, Member> members : unionVersions.values()) {
            final Set<Object> memberElementKeys = new HashSet<>();
            final Iterator<Member> i = members.values().iterator();
            Member m = null;
            while (i.hasNext()) {
                m = i.next();
                memberElementKeys.addAll(m.elementKeys());
            }
            if (memberElementKeys.containsAll(elementKeys)) {
                final List<ArtifactCoords> boms = new ArrayList<>();
                for (ArtifactCoords bom : members.keySet()) {
                    boms.add(bom);
                }
                return boms;
            }
        }
        return Collections.emptyList();
    }

}
