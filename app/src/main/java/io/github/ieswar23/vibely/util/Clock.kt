package io.github.ieswar23.vibely.util

/** Abstraction over wall-clock time so repositories and view models stay testable. */
fun interface Clock {
    fun now(): Long

    companion object {
        val System: Clock = Clock { java.lang.System.currentTimeMillis() }
    }
}
