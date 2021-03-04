/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.features

import io.ktor.features.*
import io.ktor.http.*
import io.mockk.*
import kotlin.test.*

class CORSTest {

    @Test
    fun originValidation() {
        val feature = CORS(
            CORS.Configuration().apply {
                allowSameOrigin = false
                anyHost()
            }
        )

        assertEquals(
            OriginCheckResult.OK,
            feature.checkOrigin("hyp-hen://host", getConnectionPoint("hyp-hen", "host", 123))
        )

        assertEquals(
            OriginCheckResult.OK,
            feature.checkOrigin("plus+://host", getConnectionPoint("plus+", "host", 123))
        )

        assertEquals(
            OriginCheckResult.OK,
            feature.checkOrigin("do.t://host", getConnectionPoint("do.t", "host", 123))
        )

        assertEquals(
            OriginCheckResult.OK,
            feature.checkOrigin("numbers123://host", getConnectionPoint("numbers123", "host", 123))
        )

        assertEquals(
            OriginCheckResult.TerminateSteps,
            feature.checkOrigin("1abc://host", getConnectionPoint("1abc", "host", 123))
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
