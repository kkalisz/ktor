/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.features

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Vary
import io.ktor.response.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlin.test.*

class CORSTest {
    @Test
    fun originCheckFailed() = runBlocking {
        checkCORSFailed(getConnectionPoint("http", "other", 80), "http://host")
        checkCORSFailed(getConnectionPoint("http", "other", 80), "my://host")
        checkCORSFailed(getConnectionPoint("http", "other", 80), "hyp-hen://host")
    }

    private fun getConnectionPoint(scheme: String, host: String, port: Int): RequestConnectionPoint {
        val point = mockk<RequestConnectionPoint>()
        every { point.scheme } returns scheme
        every { point.host } returns host
        every { point.port } returns port
        return point
    }

    private fun checkCORSFailed(connectionPoint: RequestConnectionPoint, origin: String) = runBlocking {
        val feature = CORS(CORS.Configuration())
        val pl = mockk<ApplicationSendPipeline> {
            coEvery { execute(any(), any()) } returns mockk()
        }

        val responseHeaders = mockk<ResponseHeaders> {
            every { this@mockk[Vary] } returns null
            every { append(any(), any()) } just Runs
        }

        val call = mockk<ApplicationCall> {
            every { response } returns mockk {
                every { headers } returns responseHeaders
                every { pipeline } returns pl
            }

            every { request } returns mockk {
                every { headers } returns mockk {
                    every { getAll(HttpHeaders.Origin) } returns listOf(origin)
                }
                every { call } returns mockk {
                    every { attributes } returns Attributes()
                }

                every { local } returns connectionPoint
            }
        }

        val context = mockk<PipelineContext<Unit, ApplicationCall>> {
            every { context } returns call
            every { finish() } just Runs
        }

        feature.intercept(context)

        verify {
            runBlocking {
                pl.execute(call, HttpStatusCode.Forbidden)
            }
        }
    }

}
