package io.quarkus.it.panache.kotlin

import jakarta.persistence.Entity
import jakarta.persistence.Transient

@Entity
open class AccessorEntity : GenericEntity<Int>() {
    var string: String? = null
    var c = 0.toChar()
    var bool = false
    var b: Byte = 0
        get() {
            getBCalls++
            return field
        }
    var s: Short = 0
    var i: Int = 0
        set(value) {
            setICalls++
            field = value
        }
    var l: Long
        get() {
            throw UnsupportedOperationException("just checking")
        }
        set(value) {
            throw UnsupportedOperationException("just checking")
        }
    var f = 0f
    var d = 0.0

    @Transient
    var trans: Any? = null

    @Transient
    var trans2: Any? = null

    // FIXME: those appear to be mapped by hibernate
    @Transient
    var getBCalls = 0

    @Transient
    var setICalls = 0

    @Transient
    var getTransCalls = 0

    @Transient
    var setTransCalls = 0

    fun method() { // touch some fields
        val b2 = b
        i = 2
        t = 1
        t2 = 2
    }
}
