package io.quarkus.bootstrap.model;

import java.util.Collection;
import java.util.Map;

public interface PlatformImports {

    /**
     * Quarkus platform properties aggregated from all the platform an application is based on.
     *
     * @return aggregated platform properties
     */
    public Map<String, String> getPlatformProperties();

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
    Collection<AppArtifactCoords> getImportedPlatformBoms();

    /**
     * In case Quarkus platform member BOM imports were misaligned this method
     * will return a detailed information about what was found to be in conflict.
     *
     * @return platform member BOM misalignment report or null, in case no conflict was detected
     */
    public String getMisalignmentReport();

    /**
     * Checks whether the platform member BOM imports belong to the same platform release.
     *
     * @return true if imported platform member BOMs belong to the same platform release, otherwise - false
     */
    public boolean isAligned();
}
