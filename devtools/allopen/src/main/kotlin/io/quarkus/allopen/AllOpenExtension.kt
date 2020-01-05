package io.quarkus.allopen

open class AllOpenExtension {
    internal val myAnnotations = mutableListOf<String>()
    internal val myPresets = mutableListOf<String>()

    open fun annotation(fqName: String) {
        myAnnotations.add(fqName)
    }

    open fun annotations(fqNames: List<String>) {
        myAnnotations.addAll(fqNames)
    }

    open fun annotations(vararg fqNames: String) {
        myAnnotations.addAll(fqNames)
    }

    open fun preset(name: String) {
        myPresets.add(name)
    }
}