package io.quarkus.vault;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.secrets.totp.CreateKeyParameters;
import io.quarkus.vault.secrets.totp.KeyConfiguration;
import io.quarkus.vault.secrets.totp.KeyDefinition;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultTOTPITCase {

    private static final String TEST_OTP_URL = "otpauth://totp/Vault:test@google.com?secret=Y64VEVMBTSXCYIWRSHRNDZW62MPGVU2G&issuer=Vault";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-totp.properties", "application.properties"));

    @Inject
    VaultTOTPSecretEngine vaultTOTPSecretEngine;

    @Test
    public void createKey() {
        CreateKeyParameters createKeyParameters = new CreateKeyParameters(
                "otpauth://totp/Vault:test@test.com?secret=Y64VEVMBTSXCYIWRSHRNDZW62MPGVU2G&issuer=Vault");
        final Optional<KeyDefinition> myKey = vaultTOTPSecretEngine.createKey("my_key", createKeyParameters);

        assertThat(myKey).isNotPresent();
    }

    @Test
    public void createGenerateKey() {
        CreateKeyParameters createKeyParameters = new CreateKeyParameters("Google", "test@gmail.com");
        final Optional<KeyDefinition> myKey = vaultTOTPSecretEngine.createKey("my_key_2", createKeyParameters);

        assertThat(myKey)
                .isPresent()
                .get().hasNoNullFieldsOrProperties();
    }

    @Test
    public void readKey() {
        CreateKeyParameters createKeyParameters = new CreateKeyParameters(
                TEST_OTP_URL);
        vaultTOTPSecretEngine.createKey("my_key_3", createKeyParameters);

        final KeyConfiguration myKey3 = vaultTOTPSecretEngine.readKey("my_key_3");
        assertThat(myKey3).returns("test@google.com", KeyConfiguration::getAccountName);
        assertThat(myKey3).returns("Vault", KeyConfiguration::getIssuer);
    }

    @Test
    public void listKeys() {
        CreateKeyParameters createKeyParameters = new CreateKeyParameters(
                TEST_OTP_URL);
        vaultTOTPSecretEngine.createKey("my_key_4", createKeyParameters);

        final List<String> listKeys = vaultTOTPSecretEngine.listKeys();
        assertThat(listKeys).contains("my_key_4");
    }

    @Test
    public void deleteKey() {
        CreateKeyParameters createKeyParameters = new CreateKeyParameters(
                TEST_OTP_URL);
        vaultTOTPSecretEngine.createKey("my_key_5", createKeyParameters);

        vaultTOTPSecretEngine.deleteKey("my_key_5");
        final List<String> listKeys = vaultTOTPSecretEngine.listKeys();
        assertThat(listKeys).doesNotContain("my_key_5");
    }

    @Test
    public void generateCode() {
        CreateKeyParameters createKeyParameters = new CreateKeyParameters(
                TEST_OTP_URL);
        vaultTOTPSecretEngine.createKey("my_key_6", createKeyParameters);

        final String myKey6Code = vaultTOTPSecretEngine.generateCode("my_key_6");
        assertThat(myKey6Code).isNotEmpty();
    }

    @Test
    public void validateCode() {
        CreateKeyParameters createKeyParameters = new CreateKeyParameters(
                TEST_OTP_URL);
        createKeyParameters.setPeriod("30m");

        vaultTOTPSecretEngine.createKey("my_key_7", createKeyParameters);
        final String myKey7Code = vaultTOTPSecretEngine.generateCode("my_key_7");

        boolean valid = vaultTOTPSecretEngine.validateCode("my_key_7", myKey7Code);
        assertThat(valid).isTrue();
    }
}
