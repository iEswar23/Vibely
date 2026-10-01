package io.github.ieswar23.vibely.data.remote.mock

import android.content.Context

/** Reads raw fixture files; abstracted so the mock server can be exercised without Android. */
fun interface AssetSource {
    fun read(fileName: String): String
}

class AndroidAssetSource(private val context: Context) : AssetSource {
    override fun read(fileName: String): String =
        context.assets.open("api/$fileName").bufferedReader().use { it.readText() }
}
