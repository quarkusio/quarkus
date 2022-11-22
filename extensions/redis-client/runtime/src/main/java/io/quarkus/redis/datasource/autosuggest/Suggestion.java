package io.quarkus.redis.datasource.autosuggest;

/**
 * Represent a suggestion.
 * <p>
 * If the {@code SUGGET} command is executed with the {@code WITHSCORES} parameter, the suggestion also contains the
 * score. {@code 0.0} otherwise.
 */
public class Suggestion {

    private final String suggestion;
    private final double score;

    public Suggestion(String suggestion, double score) {
        this.suggestion = suggestion;
        this.score = score;
    }

    public Suggestion(String suggestion) {
        this(suggestion, 0.0);
    }

    /**
     * @return the suggestion
     */
    public String suggestion() {
        return suggestion;
    }

    /**
     * @return the score, 0.0 is not available.
     */
    public double score() {
        return score;
    }
}
