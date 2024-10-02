package io.quarkus.annotation.processor.util;

import javax.annotation.processing.ProcessingEnvironment;

public final class Utils {

    private final ProcessingEnvironment processingEnv;
    private final ElementUtil elementUtil;
    private final AccessorGenerator accessorGenerator;
    private final FilerUtil filerUtil;
    private final ExtensionUtil extensionUtil;

    public Utils(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.elementUtil = new ElementUtil(processingEnv);
        this.accessorGenerator = new AccessorGenerator(processingEnv, elementUtil);
        this.filerUtil = new FilerUtil(processingEnv);
        this.extensionUtil = new ExtensionUtil(processingEnv, filerUtil);
    }

    public ElementUtil element() {
        return elementUtil;
    }

    public ProcessingEnvironment processingEnv() {
        return processingEnv;
    }

    public AccessorGenerator accessorGenerator() {
        return accessorGenerator;
    }

    public FilerUtil filer() {
        return filerUtil;
    }

    public ExtensionUtil extension() {
        return extensionUtil;
    }
}
