package model

class TargetGroup {
    var phase: String? = null
    var description: String? = null
    var order: Int = 0
    var targets: MutableList<String> = mutableListOf()
    
    constructor()
    
    constructor(phase: String?, description: String?, order: Int) {
        this.phase = phase
        this.description = description
        this.order = order
    }
    
    fun addTarget(targetName: String) {
        if (targetName !in targets) {
            targets.add(targetName)
        }
    }
}