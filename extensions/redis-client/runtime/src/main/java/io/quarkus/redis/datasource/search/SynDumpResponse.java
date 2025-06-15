package io.quarkus.redis.datasource.search;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the response of a {@code ft.syndump} command.
 */
public class SynDumpResponse {

    private final Map<String, List<String>> response;

    public SynDumpResponse(Map<String, List<String>> response) {
        this.response = response;
    }

    /**
     * Gets the list of synonyms.
     *
     * @param word
     *        the group name
     *
     * @return the list
     */
    public List<String> synonym(String word) {
        return response.get(word);
    }

    /**
     * @return the name of the groups.
     */
    public Set<String> groups() {
        return response.keySet();
    }

}
