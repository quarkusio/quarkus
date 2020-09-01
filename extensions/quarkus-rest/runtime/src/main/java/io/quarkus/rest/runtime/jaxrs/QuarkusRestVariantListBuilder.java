package io.quarkus.rest.runtime.jaxrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class QuarkusRestVariantListBuilder extends Variant.VariantListBuilder {
    private ArrayList<Variant> variants = new ArrayList<Variant>();
    private ArrayList<Locale> currentLanguages = new ArrayList<Locale>();
    private ArrayList<String> currentEncodings = new ArrayList<String>();
    private ArrayList<MediaType> currentTypes = new ArrayList<MediaType>();

    public List<Variant> build() {
        add();
        ArrayList<Variant> copy = new ArrayList<Variant>();
        copy.addAll(variants);
        variants.clear();
        currentLanguages.clear();
        currentEncodings.clear();
        currentTypes.clear();
        return copy;
    }

    public Variant.VariantListBuilder add() {
        int langSize = currentLanguages.size();
        int encodingSize = currentEncodings.size();
        int typeSize = currentTypes.size();

        int i = 0;

        if (langSize == 0 && encodingSize == 0 && typeSize == 0)
            return this;

        do {
            MediaType type = null;
            if (i < typeSize)
                type = currentTypes.get(i);
            int j = 0;
            do {
                String encoding = null;
                if (j < encodingSize)
                    encoding = currentEncodings.get(j);
                int k = 0;
                do {
                    Locale language = null;
                    if (k < langSize)
                        language = currentLanguages.get(k);
                    variants.add(new Variant(type, language, encoding));
                    k++;
                } while (k < langSize);
                j++;
            } while (j < encodingSize);
            i++;
        } while (i < typeSize);

        currentLanguages.clear();
        currentEncodings.clear();
        currentTypes.clear();

        return this;
    }

    public Variant.VariantListBuilder languages(Locale... languages) {
        for (Locale language : languages)
            currentLanguages.add(language);
        return this;
    }

    @Override
    public Variant.VariantListBuilder encodings(String... encodings) {
        for (String encoding : encodings)
            currentEncodings.add(encoding);
        return this;
    }

    @Override
    public Variant.VariantListBuilder mediaTypes(MediaType... mediaTypes) {
        for (MediaType type : mediaTypes)
            currentTypes.add(type);
        return this;
    }
}
