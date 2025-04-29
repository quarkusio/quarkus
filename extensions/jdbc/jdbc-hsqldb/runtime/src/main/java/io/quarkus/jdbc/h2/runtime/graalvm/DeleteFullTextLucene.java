package io.quarkus.jdbc.hsqldb.runtime.graalvm;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@Delete
@TargetClass(className = "org.hsqldb.fulltext.FullTextLucene")
public final class DeleteFullTextLucene {

}