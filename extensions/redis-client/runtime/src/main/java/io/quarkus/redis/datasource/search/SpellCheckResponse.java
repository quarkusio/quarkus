package io.quarkus.redis.datasource.search;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.redis.runtime.datasource.Validation;

public class SpellCheckResponse {
    private final Map<String, List<SpellCheckSuggestion>> response;

    public SpellCheckResponse(Map<String, List<SpellCheckSuggestion>> response) {
        this.response = response; // TODO remove words with a distance of 0
    }

    public List<SpellCheckSuggestion> suggestions(String name) {
        return response.get(Validation.notNullOrBlank(name, "name"));
    }

    public Set<String> misspelledWords() {
        return response.keySet();
    }

    public boolean isCorrect() {
        return response.keySet().isEmpty();
    }

    // TODO What about correctly spelled words?

    public static class SpellCheckSuggestion {

        private final String word;
        private final double distance;

        public SpellCheckSuggestion(String word, double distance) {
            this.word = word;
            this.distance = distance;
        }

        public String word() {
            return word;
        }

        public double distance() {
            return distance;
        }
    }

}
