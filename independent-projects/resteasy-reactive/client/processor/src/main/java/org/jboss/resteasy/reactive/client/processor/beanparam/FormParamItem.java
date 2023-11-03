package org.jboss.resteasy.reactive.client.processor.beanparam;

public class FormParamItem extends Item {

    private final String formParamName;
    private final String paramType;
    private final String paramSignature;
    private final String mimeType;
    private final String fileName;
    private final String sourceName;

    public FormParamItem(String fieldName, String formParamName, String paramType, String paramSignature,
            String sourceName,
            String mimeType, String fileName,
            boolean encoded,
            ValueExtractor valueExtractor) {
        super(fieldName, ItemType.FORM_PARAM, encoded, valueExtractor);
        this.formParamName = formParamName;
        this.paramType = paramType;
        this.paramSignature = paramSignature;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.sourceName = sourceName;
    }

    public String getFormParamName() {
        return formParamName;
    }

    public String getParamType() {
        return paramType;
    }

    public String getParamSignature() {
        return paramSignature;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getSourceName() {
        return sourceName;
    }
}
