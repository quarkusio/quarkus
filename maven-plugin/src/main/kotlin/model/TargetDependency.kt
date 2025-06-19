package model

class TargetDependency {
    var dependencies: Boolean? = null
    var projects: List<String>? = null
    var target: String? = null
    var params: String = "ignore"
    
    constructor()
    
    constructor(target: String) {
        this.target = target
    }
    
    constructor(target: String, dependencies: Boolean) {
        this.target = target
        this.dependencies = dependencies
    }
    
    constructor(target: String, projects: List<String>) {
        this.target = target
        this.projects = projects
    }
}