/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.features

import io.ktor.features.*
import io.ktor.http.*
import io.mockk.*
import kotlin.test.*

class CORSTest {
    private val feature: CORS = CORS(CORS.Configuration())

    @Test
    fun checkOrigin() {
        assertEquals(
            OriginCheckResult.Failed,
            feature.checkOrigin("http://host", getConnectionPoint("http", "other", 80))
        )
        assertEquals(
            OriginCheckResult.SkipCORS,
            feature.checkOrigin("invalid", getConnectionPoint("http", "other", 80))
        )
    }

    private fun getConnectionPoint(scheme: String, host: String, port: Int): RequestConnectionPoint {
        val point = mockk<RequestConnectionPoint>()
        every { point.scheme } returns scheme
        every { point.host } returns host
        every { point.port } returns port
        return point
    }
}
