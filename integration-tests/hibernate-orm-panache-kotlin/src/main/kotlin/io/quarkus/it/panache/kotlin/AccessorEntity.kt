package io.quarkus.it.panache.kotlin

import javax.persistence.Entity

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
    @JvmField
    var l: Long = 0
    var f = 0f
    var d = 0.0
    @javax.persistence.Transient
    var trans: Any? = null
    @javax.persistence.Transient
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

    fun getL(): Long = throw UnsupportedOperationException("just checking")
    fun setL(value: Long) {
        throw UnsupportedOperationException("just checking")
    }

    fun method() { // touch some fields
        val b2 = b
        i = 2
        t = 1
        t2 = 2
    }
}