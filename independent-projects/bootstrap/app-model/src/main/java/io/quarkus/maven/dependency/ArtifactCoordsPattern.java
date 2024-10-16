package io.quarkus.maven.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import io.quarkus.util.GlobUtil;

/**
 * A general purpose pattern for matching artifact coordinates (i.e. quintupless consisting of {@code groupId},
 * {@code artifactId},
 * {@code classifier}, {@code type} and {@code version}).
 * <p>
 * To create a new {@link ArtifactCoordsPattern}, use either {@link #of(String)} or {@link #builder()}, both of which accept
 * wildcard patterns (rather than regular expression patterns). See the JavaDocs of the two respective methods for more
 * details.
 * <p>
 * {@link ArtifactCoordsPattern} overrides {@link #hashCode()} and {@link #equals(Object)} and can thus be used as a key in a
 * {@link java.util.Map}.
 */
public class ArtifactCoordsPattern {

    /**
     * A {@link ArtifactCoordsPattern} builder.
     */
    public static class Builder {

        private ArtifactCoordsSegmentPattern groupIdPattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        private ArtifactCoordsSegmentPattern artifactIdPattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        private ArtifactCoordsSegmentPattern typePattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        private ArtifactCoordsSegmentPattern classifierPattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        private ArtifactCoordsSegmentPattern versionPattern = ArtifactCoordsSegmentPattern.MATCH_ALL;

        private Builder() {
        }

        public ArtifactCoordsPattern build() {
            return new ArtifactCoordsPattern(groupIdPattern, artifactIdPattern, classifierPattern, typePattern, versionPattern);
        }

        /**
         * Sets the pattern for {@code groupId}
         *
         * @param wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder setGroupId(String wildcardPattern) {
            this.groupIdPattern = new ArtifactCoordsSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code artifactId}
         *
         * @param wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder setArtifactId(String wildcardPattern) {
            this.artifactIdPattern = new ArtifactCoordsSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code classifier}
         *
         * @param wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder setClassifier(String wildcardPattern) {
            this.classifierPattern = new ArtifactCoordsSegmentPattern(wildcardPattern);
            return this;
        }

        public Builder setType(String wildcardPattern) {
            this.typePattern = new ArtifactCoordsSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code version}
         *
         * @param wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder setVersion(String wildcardPattern) {
            this.versionPattern = new ArtifactCoordsSegmentPattern(wildcardPattern);
            return this;
        }

    }

    /**
     * A pair of a {@link Pattern} and its wildcard source.
     */
    static class ArtifactCoordsSegmentPattern {
        private static final ArtifactCoordsSegmentPattern MATCH_ALL = new ArtifactCoordsSegmentPattern(
                ArtifactCoordsPattern.MULTI_WILDCARD);

        private final Pattern pattern;
        private final String source;

        ArtifactCoordsSegmentPattern(String wildcardSource) {
            super();
            this.pattern = Pattern.compile(GlobUtil.toRegexPattern(wildcardSource));
            this.source = wildcardSource;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ArtifactCoordsSegmentPattern other = (ArtifactCoordsSegmentPattern) obj;
            return source.equals(other.source);
        }

        /**
         * @return the wildcard source of the {@link #pattern}
         */
        public String getSource() {
            return source;
        }

        @Override
        public int hashCode() {
            return source.hashCode();
        }

        public boolean matches(String input) {
            if (input == null) {
                /* null input returns true only if the pattern is * */
                return MATCH_ALL.equals(this);
            }
            return pattern.matcher(input).matches();
        }

        /**
         * @return {@code true} if this {@link ArtifactCoordsSegmentPattern} is equal to {@link #MATCH_ALL}; {@code false}
         *         otherwise
         */
        public boolean matchesAll() {
            return MATCH_ALL.equals(this);
        }

        @Override
        public String toString() {
            return source;
        }
    }

    private static final char DELIMITER = ':';
    private static final String DELIMITER_STRING = String.valueOf(DELIMITER);
    private static volatile ArtifactCoordsPattern matchAll;
    private static volatile ArtifactCoordsPattern matchSnapshots;
    static final String MULTI_WILDCARD = "*";
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    /**
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    public static ArtifactCoordsPattern of(ArtifactCoords c) {
        final Builder pattern = ArtifactCoordsPattern.builder();
        pattern.setGroupId(c.getGroupId());
        pattern.setArtifactId(c.getArtifactId());
        if (c.getClassifier() != null && !c.getClassifier().isEmpty()) {
            pattern.setClassifier(c.getClassifier());
        }
        if (c.getType() != null && !c.getType().isEmpty()) {
            pattern.setType(c.getType());
        }
        return pattern.setVersion(c.getVersion()).build();
    }

    public static List<ArtifactCoordsPattern> toPatterns(Collection<ArtifactCoords> coords) {
        if (coords.isEmpty()) {
            return List.of();
        }
        final List<ArtifactCoordsPattern> result = new ArrayList<>(coords.size());
        for (ArtifactCoords c : coords) {
            result.add(of(c));
        }
        return result;
    }

    /**
     * @return a singleton that matches all possible GAVs
     */
    public static ArtifactCoordsPattern matchAll() {
        if (matchAll == null) {
            matchAll = new ArtifactCoordsPattern(
                    ArtifactCoordsSegmentPattern.MATCH_ALL,
                    ArtifactCoordsSegmentPattern.MATCH_ALL,
                    ArtifactCoordsSegmentPattern.MATCH_ALL,
                    ArtifactCoordsSegmentPattern.MATCH_ALL,
                    ArtifactCoordsSegmentPattern.MATCH_ALL);
        }
        return matchAll;
    }

    /**
     * @return a singleton that matches any GAV that has a version ending with {@value #SNAPSHOT_SUFFIX}
     */
    public static ArtifactCoordsPattern matchSnapshots() {
        if (matchSnapshots == null) {
            matchSnapshots = new ArtifactCoordsPattern(
                    ArtifactCoordsSegmentPattern.MATCH_ALL,
                    ArtifactCoordsSegmentPattern.MATCH_ALL,
                    ArtifactCoordsSegmentPattern.MATCH_ALL,
                    ArtifactCoordsSegmentPattern.MATCH_ALL,
                    new ArtifactCoordsSegmentPattern(MULTI_WILDCARD + SNAPSHOT_SUFFIX));
        }
        return matchSnapshots;
    }

    /**
     * Creates a new {@link ArtifactCoordsPattern} out of the given {@code wildcardPattern}. A wildcard pattern consists of
     * string
     * literals and asterisk wildcard {@code *}. {@code *} matches zero or many arbitrary characters. Wildcard patterns
     * for groupId, artifactId, classifier, type and version need to be delimited by colon {@value #DELIMITER}.
     * <p>
     * The general syntax of a {@link ArtifactCoordsPattern} follows the pattern
     * <code>groupIdPattern:[artifactIdPattern:[[classifierIdPattern:typePattern]:versionPattern]]</code>. Note that
     * classifier and type need to be specified both or none and that they may occur on the third and fourth position
     * respectively. Hence a {@link ArtifactCoordsPattern} with three segments {@code org.my-group:my-artifact:1.2.3} is a short
     * hand for {@code org.my-group:my-artifact:*:*:1.2.3} matching any type and any classifier.
     * <p>
     * {@link ArtifactCoordsPattern} pattern examples:
     * <p>
     * {@code org.my-group} - an equivalent of {@code org.my-group:*:*:*}. It will match any version of any artifact
     * having groupId {@code org.my-group}.
     * <p>
     * {@code org.my-group*} - an equivalent of {@code org.my-group*:*:*:*}. It will match any version of any artifact
     * whose groupId starts with {@code org.my-group} - i.e. it will match all of {@code org.my-group},
     * {@code org.my-group.api}, {@code org.my-group.impl}, etc.
     * <p>
     * {@code org.my-group:my-artifact} - an equivalent of {@code org.my-group:my-artifact:*}. It will match any version
     * of all such artifacts that have groupId {@code org.my-group} and artifactId {@code my-artifact}
     * <p>
     * {@code org.my-group:my-artifact:1.2.3} - will match just the version 1.2.3 of artifacts
     * {@code org.my-group:my-artifact}.
     * <p>
     * {@code org.my-group:my-artifact:linux-x86_64:*:1.2.3} - will match artifacts of all types having classifier
     * linux-x86_64 and version 1.2.3 of {@code org.my-group:my-artifact}.
     * <p>
     * {@code org.my-group:my-artifact::*:1.2.3} - will match artifacts of all types having no classifier and version
     * 1.2.3 of {@code org.my-group:my-artifact}.
     * <p>
     * {@code org.my-group:my-artifact:jar:1.2.3} - Illegal because both type and classifier have to be specified.
     * <p>
     * {@code org.my-group:my-artifact::jar:1.2.3} - will match the jar having no classifier and version 1.2.3 of
     * {@code org.my-group:my-artifact}.
     *
     * @param wildcardPattern a string pattern to parse and create a new {@link ArtifactCoordsPattern} from
     * @return a new {@link ArtifactCoordsPattern}
     */
    public static ArtifactCoordsPattern of(String wildcardPattern) {
        var groupIdPattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        var artifactIdPattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        var typePattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        var classifierPattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        var versionPattern = ArtifactCoordsSegmentPattern.MATCH_ALL;
        var parts = wildcardPattern.split(DELIMITER_STRING);
        if (parts.length > 0) {
            groupIdPattern = new ArtifactCoordsSegmentPattern(parts[0]);
            if (parts.length > 1) {
                artifactIdPattern = new ArtifactCoordsSegmentPattern(parts[1]);
                if (parts.length > 2) {
                    final String third = parts[2];
                    if (parts.length > 3) {
                        final String fourth = parts[3];
                        if (parts.length > 4) {
                            classifierPattern = new ArtifactCoordsSegmentPattern(third);
                            typePattern = new ArtifactCoordsSegmentPattern(fourth);
                            versionPattern = new ArtifactCoordsSegmentPattern(parts[4]);
                        } else {
                            throw new IllegalStateException(
                                    ArtifactCoordsSegmentPattern.class.getName()
                                            + ".of() expects groupId:artifactId:version or groupId:artifactId:classifier:type:version; found: "
                                            + wildcardPattern);
                        }
                    } else {
                        versionPattern = new ArtifactCoordsSegmentPattern(third);
                    }
                }
            }
        }
        return new ArtifactCoordsPattern(groupIdPattern, artifactIdPattern, classifierPattern, typePattern, versionPattern);
    }

    final ArtifactCoordsSegmentPattern groupIdPattern;
    final ArtifactCoordsSegmentPattern artifactIdPattern;
    final ArtifactCoordsSegmentPattern classifierPattern;
    final ArtifactCoordsSegmentPattern typePattern;
    final ArtifactCoordsSegmentPattern versionPattern;
    private final String source;

    ArtifactCoordsPattern(
            ArtifactCoordsSegmentPattern groupIdPattern,
            ArtifactCoordsSegmentPattern artifactIdPattern,
            ArtifactCoordsSegmentPattern classifierPattern,
            ArtifactCoordsSegmentPattern typePattern,
            ArtifactCoordsSegmentPattern versionPattern) {
        super();
        this.groupIdPattern = groupIdPattern;
        this.artifactIdPattern = artifactIdPattern;
        this.classifierPattern = classifierPattern;
        this.typePattern = typePattern;
        this.versionPattern = versionPattern;

        StringBuilder source = new StringBuilder(
                groupIdPattern.getSource().length() + artifactIdPattern.getSource().length()
                        + classifierPattern.getSource().length() + typePattern.getSource().length()
                        + versionPattern.getSource().length() + 3);

        source.append(groupIdPattern.getSource());
        final boolean artifactMatchesAll = artifactIdPattern.matchesAll();
        final boolean classifierMatchesAll = classifierPattern.matchesAll();
        final boolean typeMatchesAll = typePattern.matchesAll();
        final boolean versionMatchesAll = versionPattern.matchesAll();
        if (!versionMatchesAll) {
            source.append(DELIMITER).append(artifactIdPattern.getSource());
            if (!typeMatchesAll || !classifierMatchesAll) {
                source.append(DELIMITER).append(classifierPattern.getSource());
                source.append(DELIMITER).append(typePattern.getSource());
            }
            source.append(DELIMITER).append(versionPattern.getSource());
        } else if (!typeMatchesAll || !classifierMatchesAll) {
            source.append(DELIMITER).append(artifactIdPattern.getSource());
            source.append(DELIMITER).append(classifierPattern.getSource());
            source.append(DELIMITER).append(typePattern.getSource());
        } else if (!artifactMatchesAll) {
            source.append(DELIMITER).append(artifactIdPattern.getSource());
        }
        this.source = source.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArtifactCoordsPattern other = (ArtifactCoordsPattern) obj;
        return this.source.equals(other.source);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code type}, {@code classifier}, {@code version}
     * quintuple against this {@link ArtifactCoordsPattern}.
     *
     * @param coords artifact coordinates
     * @return {@code true} if this {@link ArtifactCoordsPattern} matches the given coordinates, otherwise - false
     */
    public boolean matches(ArtifactCoords coords) {
        return groupIdPattern.matches(coords.getGroupId()) &&
                artifactIdPattern.matches(coords.getArtifactId()) &&
                classifierPattern.matches(coords.getClassifier()) &&
                typePattern.matches(coords.getType()) &&
                versionPattern.matches(coords.getVersion());
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code type}, {@code classifier}, {@code version}
     * quintuple against this {@link ArtifactCoordsPattern}.
     *
     * @param groupId groupId
     * @param artifactId artifactId
     * @param classifier can be {@code null}
     * @param type cannot be {@code null}
     * @param version version
     * @return {@code true} if this {@link ArtifactCoordsPattern} matches the given {@code groupId}, {@code artifactId},
     *         {@code type}, {@code classifier}, {@code version} quintuple and {@code false otherwise}
     */
    public boolean matches(String groupId, String artifactId, String classifier, String type, String version) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId) && //
                classifierPattern.matches(classifier) && //
                typePattern.matches(type) && //
                versionPattern.matches(version);
    }

    @Override
    public String toString() {
        return source;
    }
}
