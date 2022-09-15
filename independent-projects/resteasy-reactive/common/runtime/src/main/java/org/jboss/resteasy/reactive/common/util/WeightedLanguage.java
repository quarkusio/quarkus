package org.jboss.resteasy.reactive.common.util;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class WeightedLanguage implements Comparable<WeightedLanguage> {
    private float weight = 1.0f;
    private String language;
    private Locale locale;
    private Map<String, String> params;

    public WeightedLanguage(final Locale locale, final float weight) {
        this.locale = locale;
        this.weight = weight;
    }

    private WeightedLanguage(final String lang, final Map<String, String> parameters) {
        this.language = lang;
        this.params = parameters;
        this.locale = LocaleHelper.extractLocale(lang);
        if (params != null) {
            String q = params.get("q");
            if (q != null) {
                weight = getQWithParamInfo(this, q);
            }
        }
    }

    public float getWeight() {
        return weight;
    }

    public Locale getLocale() {
        return locale;
    }

    public int compareTo(WeightedLanguage o) {
        WeightedLanguage type2 = this;
        WeightedLanguage type1 = o;

        if (type1.weight < type2.weight)
            return -1;
        if (type1.weight > type2.weight)
            return 1;

        return 0;
    }

    public String toString() {
        String rtn = language;
        if (params == null || params.size() == 0)
            return rtn;
        for (String name : params.keySet()) {
            String val = params.get(name);
            rtn += ";" + name + "=\"" + val + "\"";
        }
        return rtn;
    }

    public static WeightedLanguage parse(String lang) {
        String params = null;
        int idx = lang.indexOf(";");
        if (idx > -1) {
            params = lang.substring(idx + 1).trim();
            lang = lang.substring(0, idx);
        }
        HashMap<String, String> typeParams = new HashMap<String, String>();
        if (params != null && !params.equals("")) {
            int start = 0;
            while (start < params.length()) {
                start = HeaderParameterParser.setParam(typeParams, params, start);
            }
        }
        return new WeightedLanguage(lang, typeParams);
    }

    private static float getQWithParamInfo(WeightedLanguage lang, String val) {
        try {
            if (val != null) {
                float rtn = Float.valueOf(val);
                if (rtn > 1.0F)
                    throw new WebApplicationException("q value cannot be greater than one: " + (lang.toString()),
                            Response.Status.BAD_REQUEST);
                return rtn;
            }
        } catch (NumberFormatException e) {
            throw new WebApplicationException("media type weighted language q must be a float: " + (lang.toString()),
                    Response.Status.BAD_REQUEST);
        }
        return 1.0f;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
