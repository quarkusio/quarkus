package io.quarkus.websockets.next.test.kotlin

import com.fasterxml.jackson.annotation.JsonCreator

data class Message @JsonCreator constructor(var msg: String)
