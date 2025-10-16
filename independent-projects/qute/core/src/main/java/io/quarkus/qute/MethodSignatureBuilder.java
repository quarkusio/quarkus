package io.quarkus.qute;

public class MethodSignatureBuilder {

    private final StringBuilder signature;
    private int selectionStart;
    private int i = 0;
    private int selectionLength;
    private final int nbParameters;
    private boolean snippet;
    
    public MethodSignatureBuilder(String name, int nbParameters, boolean snippet) {
        signature = new StringBuilder(name);
        signature.append("(");
        this.nbParameters = nbParameters;
        this.snippet = snippet;
        this.selectionStart = nbParameters > 0 ? signature.length() : -1;
        if (nbParameters == 0) {
            signature.append(")");
        } else {
            this.selectionStart = signature.length();
        }
    }

    public void addParam(String name) {
        if (i > 0) {
            signature.append(",");
        }
        String arg = name;
        if (i == 0 && snippet) {
        	signature.append("${");
        }
        signature.append(arg);
        if (i == 0 && snippet) {
        	signature.append("}");
        }
        if (i == 0) {
            selectionLength = name.length();
        }
        if (i == nbParameters -1) {
            signature.append(")");
        }
        i++;
    }

    public int getSelectionStart() {
        return selectionStart;
    }

    public int getSelectionLength() {
        return selectionLength;
    }

    @Override
    public String toString() {
        return signature.toString();
    }
}
