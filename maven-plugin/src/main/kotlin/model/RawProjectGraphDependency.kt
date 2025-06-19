package model

class RawProjectGraphDependency {
    var source: String? = null
    var target: String? = null
    var type: DependencyType? = null
    var sourceFile: String? = null
    
    constructor()
    
    constructor(source: String, target: String, type: DependencyType) {
        this.source = source
        this.target = target
        this.type = type
    }
    
    constructor(source: String, target: String, type: DependencyType, sourceFile: String) {
        this.source = source
        this.target = target
        this.type = type
        this.sourceFile = sourceFile
    }
    
    enum class DependencyType(val value: String) {
        STATIC("static"),
        DYNAMIC("dynamic"),
        IMPLICIT("implicit");
        
        override fun toString(): String = value
    }
}