package io.quarkus.jdbc.hsqldb.runtime.graalvm;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@Delete
@TargetClass(className = "org.h2.util.SourceCompiler$GroovyCompiler")
final class DeleteGroovyCompiler {

}

@Delete
@TargetClass(className = "org.h2.util.SourceCompiler$StringJavaFileObject")
final class DeleteStringJavaFileObject {

}

@Delete
@TargetClass(className = "org.h2.util.SourceCompiler$JavaClassObject")
final class DeleteJavaClassObject {

}

@Delete
@TargetClass(className = "org.h2.util.SourceCompiler$ClassFileManager")
final class DeleteClassFileManager {

}

public final class DeleteCompilers {

}