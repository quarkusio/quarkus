package io.quarkus.it.hibernate.search.orm.elasticsearch.analysis;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Analysis0TestingEntity extends AnalysisTestingEntityBase {
    public Analysis0TestingEntity() {
    }

    public Analysis0TestingEntity(String text) {
        super(text);
    }

    @FullTextField(analyzer = "backend-level-analyzer")
    @Override
    public String getText() {
        return super.getText();
    }
}
