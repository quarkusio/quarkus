# Apache Lucene extension

## Limitations

* The extension disables the workaround for bug [JDK-4724038](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4724038) in the [MMapDirectory](https://lucene.apache.org/core/8_7_0/core/org/apache/lucene/store/MMapDirectory.html). This means that files from the index will not de deleted immediately when Lucene issues a deletion operation, but only upon garbage collection, causing a temporary larger disk usage.


* Test Coverage is currently limited to ```lucene-core``` ,```lucene-analyzers-common``` and ```lucene-queryparser``` modules.
