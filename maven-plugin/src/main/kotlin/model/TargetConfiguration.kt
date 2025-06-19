package model

class TargetConfiguration {
    var executor: String? = null
    var options: MutableMap<String, Any> = mutableMapOf()
    var inputs: MutableList<String> = mutableListOf()
    var outputs: MutableList<String> = mutableListOf()
    var dependsOn: MutableList<Any> = mutableListOf()
    var metadata: TargetMetadata? = null
    
    constructor()
    
    constructor(executor: String) {
        this.executor = executor
    }
}