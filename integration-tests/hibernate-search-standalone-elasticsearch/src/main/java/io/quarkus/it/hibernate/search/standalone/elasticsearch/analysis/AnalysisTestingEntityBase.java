package io.quarkus.it.hibernate.search.standalone.elasticsearch.analysis;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;

public class AnalysisTestingEntityBase {

    @DocumentId
    private Long id;

    private String text;

    public AnalysisTestingEntityBase(long id, String text) {
        this.id = id;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
