package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.Optional;

public class OptionalHolder {

    private Optional<String> name = Optional.empty();
    private Optional<Integer> count = Optional.empty();
    private Optional<Score> score = Optional.empty();

    public Optional<String> getName() {
        return name;
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    public Optional<Integer> getCount() {
        return count;
    }

    public void setCount(Optional<Integer> count) {
        this.count = count;
    }

    public Optional<Score> getScore() {
        return score;
    }

    public void setScore(Optional<Score> score) {
        this.score = score;
    }
}
