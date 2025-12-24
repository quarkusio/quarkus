package io.quarkus.it.jpa.mariadb;

import java.util.Locale;

import org.hibernate.dialect.MariaDBDialect;

public class OfflineDialectDescriptor {
    private Integer bytesPerCharacter;
    private Boolean noBackslashEscapes;
    private String storageEngine;

    public OfflineDialectDescriptor(MariaDBDialect dialect) {
        this(
                determineBytesPerCharacter(dialect),
                dialect.isNoBackslashEscapesEnabled(),
                determineStorageEngine(dialect));
    }

    private static String determineStorageEngine(MariaDBDialect dialect) {
        return dialect.getTableTypeString().toLowerCase(Locale.ROOT).replace("engine=", "").trim();
    }

    private static Integer determineBytesPerCharacter(MariaDBDialect dialect) {
        if (dialect.getMaxVarcharLength() == 65_535) {
            return 1;
        }
        if (dialect.getMaxVarcharLength() == 32_767) {
            return 2;
        }
        if (dialect.getMaxVarcharLength() == 21_844) {
            return 3;
        }
        return 4;
    }

    public OfflineDialectDescriptor(Integer bytesPerCharacter, Boolean noBackslashEscapes, String storageEngine) {
        this.bytesPerCharacter = bytesPerCharacter;
        this.noBackslashEscapes = noBackslashEscapes;
        this.storageEngine = storageEngine;
    }

    public Integer getBytesPerCharacter() {
        return bytesPerCharacter;
    }

    public void setBytesPerCharacter(Integer bytesPerCharacter) {
        this.bytesPerCharacter = bytesPerCharacter;
    }

    public Boolean getNoBackslashEscapes() {
        return noBackslashEscapes;
    }

    public void setNoBackslashEscapes(Boolean noBackslashEscapes) {
        this.noBackslashEscapes = noBackslashEscapes;
    }

    public String getStorageEngine() {
        return storageEngine;
    }

    public void setStorageEngine(String storageEngine) {
        this.storageEngine = storageEngine;
    }

    @Override
    public String toString() {
        return "OfflineDialectDescriptor{" +
                "bytesPerCharacter=" + bytesPerCharacter +
                ", noBackslashEscapes=" + noBackslashEscapes +
                ", storageEngine='" + storageEngine + '\'' +
                '}';
    }
}
