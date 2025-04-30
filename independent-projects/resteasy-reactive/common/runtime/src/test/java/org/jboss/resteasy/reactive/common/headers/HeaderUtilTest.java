package org.jboss.resteasy.reactive.common.headers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;
import org.junit.jupiter.api.Test;

class HeaderUtilTest {

    @Test
    void getAcceptableLanguages() {
        MultivaluedTreeMap<String, String> emptyHeaders = new MultivaluedTreeMap<>();
        Locale[] wildcardLocale = new Locale[] { new Locale("*") };
        assertArrayEquals(wildcardLocale, HeaderUtil.getAcceptableLanguages(emptyHeaders).toArray());

        MultivaluedTreeMap<String, String> singleLanguage = new MultivaluedTreeMap<>();
        singleLanguage.add("Accept-Language", "de");
        Locale[] singleLocale = new Locale[] { Locale.GERMAN };
        assertArrayEquals(singleLocale, HeaderUtil.getAcceptableLanguages(singleLanguage).toArray());

        MultivaluedTreeMap<String, String> multipleWeightedLanguages = new MultivaluedTreeMap<>();
        multipleWeightedLanguages.add("Accept-Language", "da, en-gb;q=0.8, en;q=0.7");
        Locale[] multipleWeightedLocales = new Locale[] { new Locale("da"), Locale.UK, Locale.ENGLISH };
        assertArrayEquals(multipleWeightedLocales, HeaderUtil.getAcceptableLanguages(multipleWeightedLanguages).toArray());
    }
}
