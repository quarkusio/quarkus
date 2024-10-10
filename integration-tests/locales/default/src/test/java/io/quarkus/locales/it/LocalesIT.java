package io.quarkus.locales.it;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

/**
 * For the Native test cases to function, the operating system has to have locales support installed. A barebone system with
 * only C.UTF-8 default locale available won't be able to pass the tests.
 * <p>
 * For example, this package satisfies the dependency on a RHEL 9 type of OS: glibc-all-langpacks
 */
@QuarkusIntegrationTest
public class LocalesIT {

    @Test
    public void testDefaultLocale() {
        RestAssured.given().when()
                .get("/default/de-CH")
                .then()
                .statusCode(HttpStatus.SC_OK)
                /*
                 * "l-Iżvizzera" is the correct name for Switzerland in Maltese language.
                 * Maltese is the default language as per quarkus.default-locale=mt-MT.
                 */
                .body(is("l-Iżvizzera"))
                .log().all();
    }

    /**
     * @see integration-tests/hibernate-validator/src/test/java/io/quarkus/it/hibernate/validator/HibernateValidatorFunctionalityTest.java
     */
    @ParameterizedTest
    @CsvSource(value = {
            // French locale is included, so it's used, because Croatian locale is not included
            // and thus its property file ValidationMessages_hr_HR.properties is ignored.
            "en-US;q=0.25,hr-HR;q=0.9,fr-FR;q=0.5,uk-UA;q=0.1|La valeur ne correspond pas à l'échantillon",
            // Silent fallback to lingua franca.
            "invalid string|Value is not in line with the pattern",
            // French locale is available and included.
            "en-US;q=0.25,hr-HR;q=1,fr-FR;q=0.5|La valeur ne correspond pas à l'échantillon"
    }, delimiter = '|')
    public void testValidationMessageLocale(String acceptLanguage, String expectedMessage) {
        RestAssured.given()
                .header("Accept-Language", acceptLanguage)
                .when()
                .get("/hibernate-validator-test-validation-message-locale/1")
                .then()
                .body(containsString(expectedMessage));
    }

}
