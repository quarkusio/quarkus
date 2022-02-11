package com.example.postgresql.devservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.postgresql.devservice.profile.DevServiceCustomPortProfile;
import com.example.postgresql.devservice.utils.SocketKit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServiceCustomPortProfile.class)
public class DevServicePostgresEnableITest {

    @Test
    @DisplayName("should start the postgres container when devservices is enabled with a custom port")
    public void shouldStartPostgresContainer() {
        Assertions.assertTrue(SocketKit.isPortAlreadyUsed(5432));
    }
}
