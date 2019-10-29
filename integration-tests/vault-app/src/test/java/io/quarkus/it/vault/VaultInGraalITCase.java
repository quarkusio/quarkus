package io.quarkus.it.vault;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.NativeImageTest;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@NativeImageTest
@QuarkusTestResource(VaultTestLifecycleManager.class)
@DisabledOnOs(OS.WINDOWS)
public class VaultInGraalITCase extends VaultTest {

}
