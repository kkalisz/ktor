/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util.pipeline

import io.ktor.util.collections.*
import io.ktor.utils.io.concurrent.*

internal class PhaseContent<TSubject : Any, Call : Any>(
    val phase: PipelinePhase,
    val relation: PipelinePhaseRelation,
    interceptors: MutableList<PipelineInterceptor<TSubject, Call>>
) {
    private var interceptors: MutableList<PipelineInterceptor<TSubject, Call>> by shared(interceptors)

    @Suppress("UNCHECKED_CAST")
    constructor(
        phase: PipelinePhase,
        relation: PipelinePhaseRelation
    ) : this(phase, relation, SharedArrayList as MutableList<PipelineInterceptor<TSubject, Call>>) {
        check(SharedArrayList.isEmpty()) { "The shared empty array list has been modified" }
    }

    var shared: Boolean by shared(true)

    val isEmpty: Boolean get() = interceptors.isEmpty()
    val size: Int get() = interceptors.size

    fun addInterceptor(interceptor: PipelineInterceptor<TSubject, Call>) {
        if (shared) {
            copyInterceptors()
        }

        interceptors.add(interceptor)
    }

    fun addTo(destination: MutableList<PipelineInterceptor<TSubject, Call>>) {
        val interceptors = interceptors

        if (destination is ArrayList) {
            destination.ensureCapacity(destination.size + interceptors.size)
        }

        for (index in 0 until interceptors.size) {
            destination.add(interceptors[index])
        }
    }

    fun addTo(destination: PhaseContent<TSubject, Call>) {
        if (isEmpty) return

        if (destination.isEmpty) {
            destination.interceptors = sharedInterceptors()
            destination.shared = true
            return
        }

        if (destination.shared) {
            destination.copyInterceptors()
        }

        addTo(destination.interceptors)
    }

    fun sharedInterceptors(): MutableList<PipelineInterceptor<TSubject, Call>> {
        shared = true
        return interceptors
    }

    fun copiedInterceptors(): MutableList<PipelineInterceptor<TSubject, Call>> =
        sharedListOf<PipelineInterceptor<TSubject, Call>>().apply {
            addAll(interceptors)
        }

    override fun toString(): String = "Phase `${phase.name}`, $size handlers"

    private fun copyInterceptors() {
        interceptors = copiedInterceptors()
        shared = false
    }

    companion object {
        val SharedArrayList: MutableList<Any?> = sharedListOf()
    }
}
