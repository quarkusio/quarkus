package ${package_name}

import io.quarkus.test.junit.NativeImageTest

@NativeImageTest
open class Native${class_name}IT : ${class_name}Test()
