package io.quarkus.it.hibernate.search.orm.elasticsearch.analysis;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Analysis1TestingEntity extends AnalysisTestingEntityBase {
    public Analysis1TestingEntity() {
    }

    public Analysis1TestingEntity(String text) {
        super(text);
    }

    @FullTextField(analyzer = "index-level-analyzer-1")
    @Override
    public String getText() {
        return super.getText();
    }
}
