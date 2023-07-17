package io.quarkus.deployment.configuration.matching;

import java.util.List;

import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.runtime.configuration.NameIterator;

/**
 *
 */
public final class PatternMapBuilder {

    private PatternMapBuilder() {
    }

    public static ConfigPatternMap<Container> makePatterns(List<RootDefinition> rootDefinitions) {
        ConfigPatternMap<Container> patternMap = new ConfigPatternMap<>();
        for (RootDefinition rootDefinition : rootDefinitions) {
            ConfigPatternMap<Container> addTo = patternMap, child;
            NameIterator ni = new NameIterator(rootDefinition.getName());
            assert ni.hasNext();
            do {
                final String seg = ni.getNextSegment();
                child = addTo.getChild(seg);
                ni.next();
                if (child == null) {
                    addTo.addChild(seg, child = new ConfigPatternMap<>());
                }
                addTo = child;
            } while (ni.hasNext());
            addGroup(addTo, rootDefinition, null);
        }
        return patternMap;
    }

    private static void addGroup(ConfigPatternMap<Container> patternMap, ClassDefinition current,
            Container parent) {
        for (ClassDefinition.ClassMember member : current.getMembers()) {
            final String propertyName = member.getPropertyName();
            ConfigPatternMap<Container> addTo = patternMap;
            FieldContainer newNode;
            if (!propertyName.isEmpty()) {
                NameIterator ni = new NameIterator(propertyName);
                assert ni.hasNext();
                do {
                    final String seg = ni.getNextSegment();
                    ConfigPatternMap<Container> child = addTo.getChild(seg);
                    if (child == null) {
                        addTo.addChild(seg, child = new ConfigPatternMap<>());
                    }
                    addTo = child;
                    ni.next();
                } while (ni.hasNext());
            }
            newNode = new FieldContainer(parent, member);
            addMember(addTo, member, newNode);
        }
    }

    private static void addMember(ConfigPatternMap<Container> patternMap, ClassDefinition.ClassMember member,
            Container container) {
        if (member instanceof ClassDefinition.ItemMember) {
            Container matched = patternMap.getMatched();
            if (matched != null) {
                throw new IllegalArgumentException(
                        "Multiple matching properties for name \"" + matched.getPropertyName()
                                + "\" property was matched by both " + container.findField() + " and " + matched.findField()
                                + ". This is likely because you have an incompatible combination of extensions that both define the same properties (e.g. including both reactive and blocking database extensions)");
            }
            patternMap.setMatched(container);
        } else if (member instanceof ClassDefinition.MapMember) {
            ClassDefinition.MapMember mapMember = (ClassDefinition.MapMember) member;
            ConfigPatternMap<Container> addTo = patternMap.getChild(ConfigPatternMap.WILD_CARD);
            if (addTo == null) {
                patternMap.addChild(ConfigPatternMap.WILD_CARD, addTo = new ConfigPatternMap<>());
            }
            final ClassDefinition.ClassMember nestedMember = mapMember.getNested();
            addMember(addTo, nestedMember, new MapContainer(container, nestedMember));
        } else {
            assert member instanceof ClassDefinition.GroupMember;
            addGroup(patternMap, ((ClassDefinition.GroupMember) member).getGroupDefinition(), container);
        }
    }
}
