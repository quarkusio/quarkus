package io.quarkus.it.hibernate.search.elasticsearch.analysis;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

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
