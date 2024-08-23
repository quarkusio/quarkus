package io.quarkus.locales.it;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
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

    private static final Logger LOG = Logger.getLogger(LocalesIT.class);

    @ParameterizedTest
    @CsvSource(value = {
            "en-US|en|United States",
            "de-DE|de|Deutschland",
            "de-AT|en|Austria",
            "de-DE|en|Germany"
    }, delimiter = '|')
    public void testCorrectLocales(String country, String language, String translation) {
        LOG.infof("Triggering test: Country: %s, Language: %s, Translation: %s", country, language, translation);
        RestAssured.given().when()
                .get(String.format("/locale/%s/%s", country, language))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(translation))
                .log().all();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "en-US|en|US Dollar",
            "de-DE|fr|euro",
            "cs-CZ|cs|česká koruna",
            "ja-JP|ja|日本円",
            "en-TZ|en|Tanzanian Shilling",
            "uk-UA|uk|українська гривня"
    }, delimiter = '|')
    public void testCurrencies(String country, String language, String currency) {
        LOG.infof("Triggering test: Country: %s, Language: %s, Currency: %s", country, language, currency);
        RestAssured.given().when()
                .get(String.format("/currency/%s/%s", country, language))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(currency))
                .log().all();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "Asia/Tokyo|fr|heure normale du Japon",
            "Europe/Prague|cs|Středoevropský standardní čas",
            "GMT|fr|heure moyenne de Greenwich",
            "Asia/Yerevan|ja|アルメニア標準時",
            "US/Pacific|uk|за північноамериканським тихоокеанським стандартним часом"
    }, delimiter = '|')
    public void testTimeZones(String zone, String language, String name) {
        LOG.infof("Triggering test: Zone: %s, Language: %s, Name: %s", zone, language, name);
        RestAssured.given().when()
                .param("zone", zone)
                .param("language", language)
                .get("/timeZone")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalToIgnoringCase(name))
                .log().all();
    }

    @Test
    public void testDefaultLocale() {
        RestAssured.given().when()
                .get("/default/de-CH")
                .then()
                .statusCode(HttpStatus.SC_OK)
                /*
                 * "Švýcarsko" is the correct name for Switzerland in Czech language.
                 * Czech is the default language as per quarkus.native.user-language=cs.
                 */
                .body(is("Švýcarsko"))
                .log().all();
    }

    @Test
    public void testMissingLocaleSorryItaly() {
        RestAssured.given().when()
                .get("/locale/it-IT/it")
                .then()
                .statusCode(HttpStatus.SC_OK)
                /*
                 * The expected response would be "Italia", not "Italy".
                 * Our application.properties' quarkus.native.locales property is missing "it" from the list of
                 * locales available in the native image, so the correct silent fallback is English, hence "Italy".
                 */
                .body(is("Italy"))
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
