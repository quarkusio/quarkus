package io.quarkus.it.hibernate.orm.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Cacheable
public class DataBlob {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dataBlobSeq")
    private long id;

    @Column(length = 100000)
    private String data;

    public DataBlob() {
    }

    public DataBlob(String data) {
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
