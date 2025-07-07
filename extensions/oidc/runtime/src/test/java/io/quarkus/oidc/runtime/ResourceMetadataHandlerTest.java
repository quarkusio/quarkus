package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;

public class ResourceMetadataHandlerTest {

    @Test
    public void testResourceMetadataPathDefaultTenant() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId(OidcUtils.DEFAULT_TENANT_ID)
                .resourceMetadata().enabled().end().build();
        String resourceMetadataPath = ResourceMetadataHandler.getResourceMetadataPath(oidcConfig, "/");

        assertEquals(OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH, resourceMetadataPath);
    }

    @Test
    public void testResourceMetadataPathDefaultTenantRelativeResource() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId(OidcUtils.DEFAULT_TENANT_ID)
                .resourceMetadata().enabled().resource("/metadata").end().build();
        String resourceMetadataPath = ResourceMetadataHandler.getResourceMetadataPath(oidcConfig, "/");

        assertEquals(OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH + "/metadata", resourceMetadataPath);
    }

    @Test
    public void testResourceMetadataPathDefaultTenantRelativeResource2() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId(OidcUtils.DEFAULT_TENANT_ID)
                .resourceMetadata().enabled().resource("metadata").end().build();
        String resourceMetadataPath = ResourceMetadataHandler.getResourceMetadataPath(oidcConfig, "/");

        assertEquals(OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH + "/metadata", resourceMetadataPath);
    }

    @Test
    public void testResourceMetadataPathDefaultTenantRelativeResourceSlash() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId(OidcUtils.DEFAULT_TENANT_ID)
                .resourceMetadata().enabled().resource("/").end().build();
        String resourceMetadataPath = ResourceMetadataHandler.getResourceMetadataPath(oidcConfig, "/");

        assertEquals(OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH, resourceMetadataPath);
    }

    @Test
    public void testResourceMetadataPathCustomTenant() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId("tenant")
                .resourceMetadata().enabled().end().build();
        String resourceMetadataPath = ResourceMetadataHandler.getResourceMetadataPath(oidcConfig, "/");

        assertEquals(OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH + "/tenant", resourceMetadataPath);
    }

    @Test
    public void testResourceMetadataPathAbsoluteResource() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId(OidcUtils.DEFAULT_TENANT_ID)
                .resourceMetadata().enabled().resource("https://some-ngrok-domain").end().build();
        String resourceMetadataPath = ResourceMetadataHandler.getResourceMetadataPath(oidcConfig, "/");

        assertEquals(OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH, resourceMetadataPath);
    }

    @Test
    public void testResourceMetadataPathAbsoluteResource2() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId(OidcUtils.DEFAULT_TENANT_ID)
                .resourceMetadata().enabled().resource("https://some-ngrok-domain/metadata").end().build();
        String resourceMetadataPath = ResourceMetadataHandler.getResourceMetadataPath(oidcConfig, "/");

        assertEquals(OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH + "/metadata", resourceMetadataPath);
    }

    @Test
    public void testResourceMetadataPathCustomRoot() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId(OidcUtils.DEFAULT_TENANT_ID)
                .resourceMetadata().enabled().end().build();
        String resourceMetadataPath = ResourceMetadataHandler.getResourceMetadataPath(oidcConfig, "/root");

        assertEquals("/root" + OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH, resourceMetadataPath);
    }

}
