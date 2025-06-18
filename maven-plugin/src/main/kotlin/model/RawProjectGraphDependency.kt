package model

/**
 * Represents a project graph dependency for Nx
 */
data class RawProjectGraphDependency(
    var source: String? = null,
    var target: String? = null,
    var type: DependencyType? = null,
    var sourceFile: String? = null
) {
    enum class DependencyType(private val value: String) {
        STATIC("static"),
        DYNAMIC("dynamic"), 
        IMPLICIT("implicit");
        
        override fun toString(): String = value
    }
}