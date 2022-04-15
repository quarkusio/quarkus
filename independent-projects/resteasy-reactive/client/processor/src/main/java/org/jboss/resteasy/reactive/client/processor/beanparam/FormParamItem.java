package org.jboss.resteasy.reactive.client.processor.beanparam;

public class FormParamItem extends Item {

    private final String formParamName;
    private final String paramType;

    public FormParamItem(String formParamName, String paramType, ValueExtractor valueExtractor) {
        super(ItemType.FORM_PARAM, valueExtractor);
        this.formParamName = formParamName;
        this.paramType = paramType;
    }

    public String getFormParamName() {
        return formParamName;
    }

    public String getParamType() {
        return paramType;
    }
}
