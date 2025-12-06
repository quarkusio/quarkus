package io.quarkus.bootstrap.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.maven.dependency.ArtifactCoords;

public interface PlatformImports extends Mappable {

    static PlatformImports fromMap(Map<String, Object> map) {
        return new PlatformImportsImpl(
                (Map<String, String>) map.getOrDefault(BootstrapConstants.MAPPABLE_PLATFORM_PROPS, Collections.emptyMap()),
                importedBomsFromMap(map), releaseInfoFromMap(map));
    }

    private static List<ArtifactCoords> importedBomsFromMap(Map<String, Object> map) {
        final Collection<String> importedBomsStr = (Collection<String>) map.get(BootstrapConstants.MAPPABLE_IMPORTED_BOMS);
        if (importedBomsStr != null) {
            final List<ArtifactCoords> importedBoms = new ArrayList<>(importedBomsStr.size());
            for (String importedBomStr : importedBomsStr) {
                importedBoms.add(ArtifactCoords.fromString(importedBomStr));
            }
            return importedBoms;
        }
        return Collections.emptyList();
    }

    private static List<PlatformReleaseInfo> releaseInfoFromMap(Map<String, Object> map) {
        final Collection<Map<String, Object>> releaseInfoCol = (Collection<Map<String, Object>>) map
                .get(BootstrapConstants.MAPPABLE_PLATFORM_RELEASE_INFO);
        if (releaseInfoCol != null) {
            final List<PlatformReleaseInfo> releaseInfo = new ArrayList<>(releaseInfoCol.size());
            for (Map<String, Object> releaseInfoMap : releaseInfoCol) {
                releaseInfo.add(PlatformReleaseInfo.fromMap(releaseInfoMap));
            }
            return releaseInfo;
        }
        return Collections.emptyList();
    }

    /**
     * Quarkus platform properties aggregated from all the platform an application is based on.
     *
     * @return aggregated platform properties
     */
    Map<String, String> getPlatformProperties();

    /**
     * Quarkus platform release information.
     *
     * @return platform release information
     */
    Collection<PlatformReleaseInfo> getPlatformReleaseInfo();

    /**
     * All the Quarkus platform BOMs imported by an application.
     *
     * @return all the Quarkus platform BOMs imported by an application
     */
    Collection<ArtifactCoords> getImportedPlatformBoms();

    /**
     * In case Quarkus platform member BOM imports were misaligned this method
     * will return detailed information about what was found to be in conflict.
     *
     * @return platform member BOM misalignment report or null, in case no conflict was detected
     */
    String getMisalignmentReport();

    /**
     * Checks whether the platform member BOM imports belong to the same platform release.
     *
     * @return true if imported platform member BOMs belong to the same platform release, otherwise - false
     */
    boolean isAligned();

    @Override
    default Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap(3);

        if (!getPlatformProperties().isEmpty()) {
            final Map<String, String> props = getPlatformProperties();
            var jsonProps = factory.newMap(props.size());
            jsonProps.putAll(props);
            map.put(BootstrapConstants.MAPPABLE_PLATFORM_PROPS, jsonProps);
        }

        if (getPlatformReleaseInfo() != null) {
            map.put(BootstrapConstants.MAPPABLE_PLATFORM_RELEASE_INFO, Mappable.asMaps(getPlatformReleaseInfo(), factory));
        }

        if (getImportedPlatformBoms() != null) {
            map.put(BootstrapConstants.MAPPABLE_IMPORTED_BOMS,
                    Mappable.toStringCollection(getImportedPlatformBoms(), ArtifactCoords::toGACTVString, factory));
        }
        return map;
    }
}
