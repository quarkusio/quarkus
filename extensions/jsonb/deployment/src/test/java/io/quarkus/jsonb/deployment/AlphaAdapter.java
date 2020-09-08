package io.quarkus.jsonb.deployment;

import javax.inject.Inject;
import javax.json.bind.adapter.JsonbAdapter;

// scope annotation is added automatically
public class AlphaAdapter implements JsonbAdapter<Alpha, String> {

    @Inject
    Bravo bravo;

    @Override
    public String adaptToJson(Alpha obj) throws Exception {
        return bravo.getVal(obj);
    }

    @Override
    public Alpha adaptFromJson(String obj) throws Exception {
        return new Alpha(bravo.getVal(obj));
    }

}
