package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;

class CpeArtifactsEncoderTest {

    @Test
    void roundTripsRuntimeKeyedClosure() {
        // By convention the deployment closure contains the runtime artifact, so the key appears in its value.
        ArtifactCoords rt = ArtifactCoords.jar("org.acme", "acme-core", "1.0");
        ArtifactCoords deployment = ArtifactCoords.jar("org.acme", "acme-core-deployment", "1.0");
        ArtifactCoords quarkusCore = ArtifactCoords.jar("io.quarkus", "quarkus-core", "3.15.0");

        String encoded = CpeArtifactsEncoder.encode(Map.of(rt, List.of(rt, deployment, quarkusCore)));
        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(encoded);

        assertThat(decoded).containsOnlyKeys(rt);
        assertThat(decoded.get(rt)).containsExactlyInAnyOrder(rt, deployment, quarkusCore);
    }

    @Test
    void roundTripsSharedDependenciesAcrossEntries() {
        var map = sampleMap();

        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(CpeArtifactsEncoder.encode(map));

        assertThat(decoded).containsOnlyKeys(map.keySet().toArray(ArtifactCoords[]::new));
        for (var entry : map.entrySet()) {
            assertThat(decoded.get(entry.getKey())).containsExactlyInAnyOrderElementsOf(entry.getValue());
        }
    }

    @Test
    void entriesAreOrderedByRuntimeKey() {
        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(CpeArtifactsEncoder.encode(sampleMap()));

        var keys = List.copyOf(decoded.keySet());
        for (int i = 1; i < keys.size(); i++) {
            assertThat(keys.get(i - 1).toGACTVString()).isLessThan(keys.get(i).toGACTVString());
        }
    }

    @Test
    void handlesNonJarAndClassifiedCoordinates() {
        ArtifactCoords rt = ArtifactCoords.jar("org.acme", "acme-rt", "1.0");
        ArtifactCoords bom = ArtifactCoords.pom("org.acme", "acme-bom", "1.0");
        ArtifactCoords classified = ArtifactCoords.of("org.acme", "acme-natives", "linux-x86_64", "jar", "1.0");

        String encoded = CpeArtifactsEncoder.encode(Map.of(rt, List.of(rt, bom, classified)));
        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(encoded);

        assertThat(decoded.get(rt)).containsExactlyInAnyOrder(rt, bom, classified);
    }

    @Test
    void roundTripsSharedArtifactIdPrefixWithinGroup() {
        // Artifacts of the same project share a common artifactId prefix that is factored out.
        ArtifactCoords rt = ArtifactCoords.jar("org.apache.camel.quarkus", "camel-quarkus-core", "1.0");
        ArtifactCoords a = ArtifactCoords.jar("org.apache.camel.quarkus", "camel-quarkus-jackson", "1.0");
        ArtifactCoords b = ArtifactCoords.of("org.apache.camel.quarkus", "camel-quarkus-support", "linux-x86_64", "jar", "1.0");

        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(
                CpeArtifactsEncoder.encode(Map.of(rt, List.of(rt, a, b))));

        assertThat(decoded.get(rt)).containsExactlyInAnyOrder(rt, a, b);
    }

    @Test
    void roundTripsMultipleVersionsInSameGroup() {
        ArtifactCoords rt = ArtifactCoords.jar("org.acme", "acme-a", "2.0");
        ArtifactCoords older = ArtifactCoords.jar("org.acme", "acme-a-legacy", "1.0");
        ArtifactCoords newer = ArtifactCoords.jar("org.acme", "acme-a-deployment", "2.0");

        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(
                CpeArtifactsEncoder.encode(Map.of(rt, List.of(rt, older, newer))));

        assertThat(decoded.get(rt)).containsExactlyInAnyOrder(rt, older, newer);
    }

    @Test
    void roundTripsSingletonGroup() {
        ArtifactCoords rt = ArtifactCoords.jar("io.quarkus", "quarkus-core", "3.15.0");

        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(
                CpeArtifactsEncoder.encode(Map.of(rt, List.of(rt))));

        assertThat(decoded.get(rt)).containsExactly(rt);
    }

    @Test
    void roundTripsArtifactIdEqualToGroupPrefix() {
        // The group prefix equals one of the artifactIds, which serializes as an empty artifact line.
        ArtifactCoords rt = ArtifactCoords.jar("io.quarkus", "quarkus-core", "1.0");
        ArtifactCoords deployment = ArtifactCoords.jar("io.quarkus", "quarkus-core-deployment", "1.0");

        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(
                CpeArtifactsEncoder.encode(Map.of(rt, List.of(rt, deployment))));

        assertThat(decoded.get(rt)).containsExactlyInAnyOrder(rt, deployment);
    }

    @Test
    void roundTripsArtifactIdEqualToGroupPrefixWithClassifier() {
        // Same artifactId as the prefix, but carrying a classifier: the empty remainder still keeps the suffix.
        ArtifactCoords rt = ArtifactCoords.jar("org.acme", "acme", "1.0");
        ArtifactCoords classified = ArtifactCoords.of("org.acme", "acme", "linux-x86_64", "jar", "1.0");
        ArtifactCoords sibling = ArtifactCoords.jar("org.acme", "acme-extra", "1.0");

        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(
                CpeArtifactsEncoder.encode(Map.of(rt, List.of(rt, classified, sibling))));

        assertThat(decoded.get(rt)).containsExactlyInAnyOrder(rt, classified, sibling);
    }

    @Test
    void roundTripsSpreadOutDeltaEncodedIndices() {
        // A closure that references dictionary entries scattered across many groups exercises delta decoding.
        ArtifactCoords rt = ArtifactCoords.jar("org.acme", "acme-core", "1.0");
        var value = new ArrayList<ArtifactCoords>();
        value.add(rt);
        for (int i = 0; i < 50; i++) {
            value.add(ArtifactCoords.jar("org.dep" + i, "lib-" + i, "1." + i));
        }

        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(
                CpeArtifactsEncoder.encode(Map.of(rt, value)));

        assertThat(decoded.get(rt)).containsExactlyInAnyOrderElementsOf(value);
    }

    @Test
    void serializesToExpectedGroupedFormat() {
        ArtifactCoords rt = ArtifactCoords.jar("org.acme", "acme-a", "1.0");
        ArtifactCoords deployment = ArtifactCoords.jar("org.acme", "acme-a-deployment", "1.0");
        ArtifactCoords shared = ArtifactCoords.jar("org.shared", "shared", "2.0");

        String serialized = CpeArtifactsEncoder.serialize(Map.of(rt, List.of(rt, deployment, shared)));

        assertThat(serialized).isEqualTo(""
                + "@org.acme\n"
                + "acme-a\n" // group prefix (LCP of acme-a and acme-a-deployment)
                + "=1.0\n"
                + "\n" // acme-a: remainder equals the prefix
                + "-deployment\n" // acme-a-deployment
                + "@org.shared\n"
                + "shared\n" // group prefix (single artifact)
                + "=2.0\n"
                + "\n" // shared: remainder equals the prefix
                + "--\n"
                + "0[0,1,1]\n"); // key index 0; deps at indices 0,1,2 delta-encoded
    }

    @Test
    void encodingIsDeterministicRegardlessOfInputOrder() {
        var natural = sampleMap();

        // A copy with entries and dependency lists shuffled, backed by a differently-ordered map.
        var entries = new ArrayList<>(natural.entrySet());
        Collections.shuffle(entries, new Random(1));
        var shuffled = new LinkedHashMap<ArtifactCoords, List<ArtifactCoords>>();
        for (var e : entries) {
            var deps = new ArrayList<>(e.getValue());
            Collections.shuffle(deps, new Random(2));
            shuffled.put(e.getKey(), deps);
        }

        assertThat(CpeArtifactsEncoder.encode(shuffled)).isEqualTo(CpeArtifactsEncoder.encode(natural));
    }

    @Test
    void encodedStringIsBase64() {
        ArtifactCoords rt = ArtifactCoords.jar("org.acme", "acme", "1.0");
        String encoded = CpeArtifactsEncoder.encode(Map.of(rt, List.of(rt)));
        assertThat(encoded).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void roundTripEmptyMap() {
        String encoded = CpeArtifactsEncoder.encode(Map.of());
        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(encoded);
        assertThat(decoded).isEmpty();
    }

    @Test
    void decodesEmptyStringToEmptyMap() {
        assertThat(CpeArtifactsEncoder.decode("")).isEmpty();
    }

    private static Map<ArtifactCoords, List<ArtifactCoords>> sampleMap() {
        // Each entry is keyed by a runtime artifact whose deployment closure (the value) contains it,
        // with some dependencies shared across entries to exercise the dictionary.
        ArtifactCoords rtA = ArtifactCoords.jar("org.acme", "acme-a", "1.0");
        ArtifactCoords rtB = ArtifactCoords.jar("org.acme", "acme-b", "1.0");
        ArtifactCoords rtC = ArtifactCoords.jar("org.acme", "acme-c", "1.0");
        ArtifactCoords shared1 = ArtifactCoords.jar("org.shared", "shared-1", "2.0");
        ArtifactCoords shared2 = ArtifactCoords.jar("org.shared", "shared-2", "2.0");
        ArtifactCoords onlyA = ArtifactCoords.jar("org.acme", "acme-a-deployment", "1.0");
        ArtifactCoords onlyB = ArtifactCoords.jar("org.acme", "acme-b-deployment", "1.0");

        var map = new LinkedHashMap<ArtifactCoords, List<ArtifactCoords>>();
        map.put(rtA, List.of(rtA, onlyA, shared1, shared2));
        map.put(rtB, List.of(rtB, onlyB, shared1, shared2));
        map.put(rtC, List.of(rtC, shared1));
        return map;
    }
}
