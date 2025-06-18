package model

/**
 * Represents an Nx target configuration
 */
data class TargetConfiguration(
    var executor: String? = null,
    var options: MutableMap<String, Any> = linkedMapOf(),
    var inputs: MutableList<String> = mutableListOf(),
    var outputs: MutableList<String> = mutableListOf(),
    var dependsOn: MutableList<Any> = mutableListOf(),
    var metadata: TargetMetadata? = null
) {
    constructor(executor: String) : this() {
        this.executor = executor
    }
}