package model

class TargetMetadata {
    var type: String? = null
    var phase: String? = null
    var plugin: String? = null
    var goal: String? = null
    var executionId: String? = null
    var technologies: MutableList<String> = mutableListOf()
    var description: String? = null
    
    constructor()
    
    constructor(type: String, description: String) {
        this.type = type
        this.description = description
    }
}