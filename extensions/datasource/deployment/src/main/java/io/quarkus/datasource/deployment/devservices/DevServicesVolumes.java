package io.quarkus.datasource.deployment.devservices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.datasource.common.devservices.VolumeMount;
import io.quarkus.datasource.runtime.DevServicesBuildTimeConfig;

final class DevServicesVolumes {

    private static final String CLASSPATH = "classpath:";
    private static final Set<String> VOLUME_PROPERTIES = Set.of("type", "source", "target", "read-only");

    private DevServicesVolumes() {
    }

    static List<VolumeMount> toVolumeMounts(DevServicesBuildTimeConfig config) {
        return toVolumeMounts(config.volumes(), config.namedVolumes());
    }

    static List<VolumeMount> toVolumeMounts(Map<String, String> legacyVolumes,
            Map<String, ? extends DevServicesBuildTimeConfig.Volume> namedVolumes) {
        List<VolumeMount> result = new ArrayList<>(legacyVolumes.size() + namedVolumes.size());
        for (Map.Entry<String, String> volume : legacyVolumes.entrySet()) {
            String hostLocation = volume.getKey();
            if (isNamedVolumeProperty(hostLocation, namedVolumes.keySet())) {
                continue;
            }
            if (hostLocation.startsWith(CLASSPATH)) {
                result.add(VolumeMount.classpath(hostLocation.substring(CLASSPATH.length()), volume.getValue(), true));
            } else {
                result.add(VolumeMount.bind(hostLocation, volume.getValue(), false));
            }
        }
        for (DevServicesBuildTimeConfig.Volume volume : namedVolumes.values()) {
            VolumeMount.Type type = switch (volume.type()) {
                case BIND -> VolumeMount.Type.BIND;
                case CLASSPATH -> VolumeMount.Type.CLASSPATH;
            };
            boolean readOnly = volume.readOnly().orElse(type == VolumeMount.Type.CLASSPATH);
            result.add(new VolumeMount(type, volume.source(), volume.target(), readOnly));
        }
        return result;
    }

    /**
     * Both forms of {@code quarkus.datasource.devservices.volumes} share a prefix, and a {@code Map<String, String>}
     * mapping collects every property under its prefix whatever the number of remaining key segments. The
     * properties of a named volume are therefore also present in the legacy map, as {@code name.source} or
     * {@code "name".source} entries, and must not be interpreted as host locations.
     */
    private static boolean isNamedVolumeProperty(String legacyKey, Set<String> volumeNames) {
        int lastDot = legacyKey.lastIndexOf('.');
        if (lastDot < 0 || !VOLUME_PROPERTIES.contains(legacyKey.substring(lastDot + 1))) {
            return false;
        }
        String name = legacyKey.substring(0, lastDot);
        if (name.length() > 1 && name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }
        return volumeNames.contains(name);
    }
}
