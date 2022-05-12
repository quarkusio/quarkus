package io.quarkus.locales.it;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.NativeImageTest;
import io.restassured.RestAssured;

/**
 * For the Native test cases to function, the operating system has to have locales
 * support installed. A barebone system with only C.UTF-8 default locale available
 * won't be able to pass the tests.
 *
 * For example, this package satisfies the dependency on a RHEL 9 type of OS:
 * glibc-all-langpacks
 *
 */
@NativeImageTest
public class LocalesIT {

    private static final Logger LOG = Logger.getLogger(LocalesIT.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "en-US|en|United States",
            "de-DE|de|Deutschland",
            "de-AT|en|Austria",
            "de-DE|en|Germany"
    })
    public void testCorrectLocales(String countryLanguageTranslation) {
        final String[] lct = countryLanguageTranslation.split("\\|");
        LOG.infof("Triggering test: Country: %s, Language: %s, Translation: %s", lct[0], lct[1], lct[2]);
        RestAssured.given().when()
                .get(String.format("/locale/%s/%s", lct[0], lct[1]))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(lct[2]))
                .log().all();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "en-US|en|US Dollar",
            "de-DE|fr|euro",
            "cs-CZ|cs|česká koruna",
            "ja-JP|ja|日本円",
            "en-TZ|en|Tanzanian Shilling",
            "uk-UA|uk|українська гривня"
    })
    public void testCurrencies(String countryLanguageCurrency) {
        final String[] clc = countryLanguageCurrency.split("\\|");
        LOG.infof("Triggering test: Country: %s, Language: %s, Currency: %s", clc[0], clc[1], clc[2]);
        RestAssured.given().when()
                .get(String.format("/currency/%s/%s", clc[0], clc[1]))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(clc[2]))
                .log().all();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Asia/Tokyo|fr|heure normale du Japon",
            "Europe/Prague|cs|Středoevropský standardní čas",
            "GMT|fr|heure moyenne de Greenwich",
            "Asia/Yerevan|ja|アルメニア標準時",
            "US/Pacific|uk|за північноамериканським тихоокеанським стандартним часом"
    })
    public void testTimeZones(String zoneLanguageName) {
        final String[] zln = zoneLanguageName.split("\\|");
        LOG.infof("Triggering test: Zone: %s, Language: %s, Name: %s", zln[0], zln[1], zln[2]);
        RestAssured.given().when()
                .param("zone", zln[0])
                .param("language", zln[1])
                .get("/timeZone")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(zln[2]))
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
}
