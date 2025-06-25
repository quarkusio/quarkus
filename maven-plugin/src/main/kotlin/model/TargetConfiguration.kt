package model

// Force recompilation by adding timestamp: 2025-06-25T17:05:00Z
class TargetConfiguration(
    var executor: String? = null
) {
    var options: MutableMap<String, Any> = mutableMapOf()
    var inputs: MutableList<String> = mutableListOf()
    var outputs: MutableList<String> = mutableListOf()
    var dependsOn: MutableList<Any> = mutableListOf()
    var metadata: TargetMetadata? = null
    
    var cache: Boolean? = null
}