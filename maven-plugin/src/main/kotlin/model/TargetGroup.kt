package model

/**
 * Represents a group of targets organized by Maven lifecycle phase
 */
data class TargetGroup(
    var phase: String? = null,
    var description: String? = null,
    var targets: MutableList<String> = mutableListOf(),
    var order: Int = 0
) {
    constructor(phase: String, description: String, order: Int) : this() {
        this.phase = phase
        this.description = description
        this.order = order
    }
    
    fun addTarget(targetName: String) {
        if (!targets.contains(targetName)) {
            targets.add(targetName)
        }
    }
}