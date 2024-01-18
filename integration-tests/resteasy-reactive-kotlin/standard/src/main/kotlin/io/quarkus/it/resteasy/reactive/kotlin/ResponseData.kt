package io.quarkus.it.resteasy.reactive.kotlin

class ResponseData<T>(
    val code: Int = STATUS_CODE.SUCCESS.code,
    val msg: String = "",
    val data: T? = null,
) {
    companion object {
        fun success() = ResponseData<Unit>()
        fun failure(msg: String) = ResponseData<Unit>(code = STATUS_CODE.ERROR.code, msg = msg)
    }
}

enum class STATUS_CODE(val code: Int) {
    SUCCESS(200),
    ERROR(500)
}
