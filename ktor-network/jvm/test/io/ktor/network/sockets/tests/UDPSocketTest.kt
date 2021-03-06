/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets.tests

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.util.network.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.debug.junit4.*
import org.junit.*
import java.lang.IllegalStateException
import java.net.*
import kotlin.coroutines.*
import kotlin.io.use
import kotlin.test.*
import kotlin.test.Test

class UDPSocketTest : CoroutineScope {
    private val testJob = Job()
    private val selector = ActorSelectorManager(Dispatchers.Default + testJob)

    @get:Rule
    val timeout = CoroutinesTimeout(1000, cancelOnTimeout = true)

    override val coroutineContext: CoroutineContext
        get() = testJob

    @AfterTest
    fun tearDown() {
        testJob.cancel()
        selector.close()
    }

    @Test
    fun testBroadcastFails(): Unit = runBlocking {
        if (OS_NAME == "win") {
            return@runBlocking
        }

        retryIgnoringBindException {
            lateinit var socket: BoundDatagramSocket
            var denied = false
            try {
                socket = aSocket(selector)
                    .udp()
                    .bind()

                socket.use {
                    val datagram = Datagram(
                        packet = buildPacket { writeText("0123456789") },
                        address = NetworkAddress("255.255.255.255", 56700)
                    )

                    it.send(datagram)
                }
            } catch (cause: SocketException) {
                if (!cause.message.equals("Permission denied", ignoreCase = true)) {
                    throw cause
                }

                denied = true
            }

            assertTrue(denied)
            socket.socketContext.join()
            assertTrue(socket.isClosed)
        }
    }

    @Test
    fun testClose(): Unit = runBlocking {
        retryIgnoringBindException {
            val socket = aSocket(selector)
                .udp()
                .bind()

            socket.close()

            socket.socketContext.join()
            assertTrue(socket.isClosed)
        }
    }

    @Test
    fun testInvokeOnClose() = runBlocking {
        retryIgnoringBindException {
            val socket: BoundDatagramSocket = aSocket(selector)
                .udp()
                .bind()

            var done = 0
            socket.outgoing.invokeOnClose {
                done += 1
            }

            assertFailsWith<IllegalStateException> {
                socket.outgoing.invokeOnClose {
                    done += 2
                }
            }

            socket.close()
            socket.close()

            socket.socketContext.join()
            assertTrue(socket.isClosed)
            assertEquals(1, done)
        }
    }

    @Test
    fun testOutgoingInvokeOnClose() = runBlocking {
        retryIgnoringBindException {
            val socket: BoundDatagramSocket = aSocket(selector)
                .udp()
                .bind()

            var done = 0
            socket.outgoing.invokeOnClose {
                done += 1
                assertTrue(it is AssertionError)
            }

            socket.outgoing.close(AssertionError())

            assertEquals(1, done)
            socket.socketContext.join()
            assertTrue(socket.isClosed)
        }
    }

    @Test
    fun testOutgoingInvokeOnCloseWithSocketClose() = runBlocking {
        retryIgnoringBindException {
            val socket: BoundDatagramSocket = aSocket(selector)
                .udp()
                .bind()

            var done = 0
            socket.outgoing.invokeOnClose {
                done += 1
            }

            socket.close()

            assertEquals(1, done)

            socket.socketContext.join()
            assertTrue(socket.isClosed)
        }
    }

    @Test
    fun testOutgoingInvokeOnClosed() = runBlocking {
        retryIgnoringBindException {
            val socket: BoundDatagramSocket = aSocket(selector)
                .udp()
                .bind()

            socket.outgoing.close(AssertionError())

            var done = 0
            socket.outgoing.invokeOnClose {
                done += 1
                assertTrue(it is AssertionError)
            }

            assertEquals(1, done)

            socket.socketContext.join()
            assertTrue(socket.isClosed)
        }
    }
}

internal inline fun retryIgnoringBindException(block: () -> Unit) {
    var done = false
    while (!done) {
        try {
            block()
        } catch (cause: SocketException) {
            if (!cause.message.equals("Already bound", ignoreCase = true)) {
                throw cause
            }
        }

        done = true
    }
}

private val OS_NAME: String
    get() {
        val os = System.getProperty("os.name", "unknown").toLowerCase()
        return when {
            os.contains("win") -> "win"
            os.contains("mac") -> "mac"
            os.contains("nux") -> "unix"
            else -> "unknown"
        }
    }
