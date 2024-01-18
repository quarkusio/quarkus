package io.quarkus.locales.it;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

/**
 * A special case where we want to include all locales in our app. It must not matter which arbitrary locale we use, it must
 * work here.
 */
@QuarkusIntegrationTest
public class LocalesIT {

    private static final Logger LOG = Logger.getLogger(LocalesIT.class);

    @ParameterizedTest
    @CsvSource(value = {
            "en-US|en|United States",
            "de-DE|de|Deutschland",
            "de-AT|en|Austria",
            "de-DE|en|Germany",
            "zh-cmn-Hans-CN|cs|Čína",
            "zh-Hant-TW|cs|Tchaj-wan",
            "ja-JP-JP-#u-ca-japanese|sg|Zapöon"
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

    @Test
    public void testItalyIncluded() {
        RestAssured.given().when()
                .get("/locale/it-IT/it")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("Italia"))
                .log().all();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "0,666|en-US|666.0",
            "0,666|cs-CZ|0.666",
            "0,666|fr-FR|0.666",
            "0.666|fr-FR|0.0"
    }, delimiter = '|')
    public void testNumbers(String number, String locale, String expected) {
        LOG.infof("Triggering test: Number: %s, Locale: %s, Expected result: %s", number, locale, expected);
        RestAssured.given().when()
                .param("number", number)
                .param("locale", locale)
                .get("/numbers")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected))
                .log().all();
    }

    @Test
    public void languageRanges() {
        RestAssured.given().when()
                .param("range", "Accept-Language:iw,en-us;q=0.7,en;q=0.3")
                .get("/ranges")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("[iw, he, en-us;q=0.7, en;q=0.3]"))
                .log().all();
    }

    @ParameterizedTest
    @CsvSource(value = {
            // Ukrainian language preference is higher than Czech.
            "cs;q=0.7,uk;q=0.9|Привіт Світ!",
            // Czech language preference is higher than Ukrainian.
            "cs;q=1.0,uk;q=0.9|Ahoj světe!",
            // An unknown language preference, silent fallback to lingua franca.
            "jp;q=1.0|Hello world!"
    }, delimiter = '|')
    public void message(String acceptLanguage, String expectedMessage) {
        RestAssured.given().when()
                .header("Accept-Language", acceptLanguage)
                .get("/message")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(expectedMessage))
                .log().all();
    }

    /**
     * @see integration-tests/hibernate-validator/src/test/java/io/quarkus/it/hibernate/validator/HibernateValidatorFunctionalityTest.java
     */
    @ParameterizedTest
    @CsvSource(value = {
            // Croatian language preference is higher than Ukrainian.
            "en-US;q=0.25,hr-HR;q=0.9,fr-FR;q=0.5,uk-UA;q=0.1|Vrijednost ne zadovoljava uzorak",
            // Ukrainian language preference is higher than Croatian.
            "en-US;q=0.25,hr-HR;q=0.9,fr-FR;q=0.5,uk-UA;q=1.0|Значення не відповідає зразку",
            // An unknown language preference, silent fallback to lingua franca.
            "invalid string|Value is not in line with the pattern",
            // Croatian language preference is the highest.
            "en-US;q=0.25,hr-HR;q=1,fr-FR;q=0.5|Vrijednost ne zadovoljava uzorak",
            // Chinese language preference is the highest.
            "en-US;q=0.25,hr-HR;q=0.30,zh;q=0.9,fr-FR;q=0.50|數值不符合樣品",
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
