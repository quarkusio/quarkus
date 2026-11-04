package org.acme.invokedyn;

public interface LambdaConstant {

    // The lambda is cast to a non-public sub-interface, so TaggedLambda only appears in
    // the invokedynamic call site descriptor: ()Lorg/acme/invokedyn/TaggedLambda;
    LambdaConstant INSTANCE = (TaggedLambda) () -> "tagged";

    String value();
}
