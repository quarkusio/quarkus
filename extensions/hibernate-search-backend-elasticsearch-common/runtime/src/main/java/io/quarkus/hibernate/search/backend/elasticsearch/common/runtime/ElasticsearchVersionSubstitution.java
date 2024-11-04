package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import io.quarkus.runtime.ObjectSubstitution;

public class ElasticsearchVersionSubstitution implements ObjectSubstitution<ElasticsearchVersion, String> {
    @Override
    public String serialize(ElasticsearchVersion obj) {
        return obj.toString();
    }

    @Override
    public ElasticsearchVersion deserialize(String obj) {
        return ElasticsearchVersion.of(obj);
    }
}
