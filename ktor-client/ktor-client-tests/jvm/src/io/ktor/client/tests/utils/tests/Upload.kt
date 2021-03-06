/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests.utils.tests

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

public fun Application.uploadTest() {
    routing {
        route("upload") {
            post("content") {
                val message = call.request.headers[HttpHeaders.ContentType] ?: "EMPTY"
                call.respond(HttpStatusCode.OK, message)
            }
        }
    }
}
