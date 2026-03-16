package io.quarkus.it.vector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class VectorEntity {

    @Id
    private Long id;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(3)")
    private float[] embedding;

    public VectorEntity() {
    }

    public VectorEntity(Long id, float[] embedding) {
        this.id = id;
        this.embedding = embedding;
    }

    public Long getId() {
        return id;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}
