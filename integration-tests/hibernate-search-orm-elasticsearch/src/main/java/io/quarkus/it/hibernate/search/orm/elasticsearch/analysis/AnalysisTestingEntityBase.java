package io.quarkus.it.hibernate.search.orm.elasticsearch.analysis;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class AnalysisTestingEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analysisSeq")
    private Long id;

    private String text;

    public AnalysisTestingEntityBase() {
    }

    public AnalysisTestingEntityBase(String text) {
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
