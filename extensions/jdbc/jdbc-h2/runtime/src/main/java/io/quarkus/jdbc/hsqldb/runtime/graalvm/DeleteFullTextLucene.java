package io.quarkus.jdbc.hsqldb.runtime.graalvm;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@Delete
@TargetClass(className = "org.h2.fulltext.FullTextLucene")
public final class DeleteFullTextLucene {

}