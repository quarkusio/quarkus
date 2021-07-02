package io.quarkus.devtools.generators.kinds.model;

import java.util.List;

public class Model {

    private String className;
    private String importModel;
    private String importModelLowerCase;
    private String tableName;
    private String packageValue;
    private List<Attribute> attributes;

    public Model(String className, String packageValue, String importModel) {
        this.className = className;
        this.packageValue = packageValue;
        this.importModel = importModel;
    }

    public Model(String className, List<Attribute> attrs, String packageValue, String tableName) {
        this.className = className;
        this.attributes = attrs;
        this.packageValue = packageValue;
        this.tableName = tableName;
    }

    public String getImportModelLowerCase() {
        return importModel.toLowerCase();
    }

    public String getImportModel() {
        return importModel;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageValue() {
        return packageValue;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public String getTableName() {
        return tableName;
    }

    static class Attribute {
        public String type;
        private String name;

        public Attribute(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
