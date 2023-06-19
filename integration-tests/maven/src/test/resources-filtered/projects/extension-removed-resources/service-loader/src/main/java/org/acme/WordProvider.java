package org.acme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public interface WordProvider {

    static List<WordProvider> loadProviders() {
        var i = ServiceLoader.load(WordProvider.class).iterator();
        if(!i.hasNext()) {
            return List.of();
        }
        final List<WordProvider> result = new ArrayList<>();
        while(i.hasNext()) {
            result.add(i.next());
        }
        return result;
    }

    static List<String> loadAndSortWords() {
        var providers = loadProviders();
        if(providers.isEmpty()) {
            return List.of();
        }
        final List<String> result = new ArrayList<>(providers.size());
        for(var provider : providers) {
            result.add(provider.getWord());
        }
        Collections.sort(result);
        return result;
    }

    String getWord();
}