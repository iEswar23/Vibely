package io.github.ieswar23.vibely.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.ieswar23.vibely.data.local.entity.StoryFrameEntity

class Converters {
    private val gson = Gson()
    private val framesType = object : TypeToken<List<StoryFrameEntity>>() {}.type

    @TypeConverter
    fun framesToJson(frames: List<StoryFrameEntity>): String = gson.toJson(frames)

    @TypeConverter
    fun jsonToFrames(json: String): List<StoryFrameEntity> =
        gson.fromJson<List<StoryFrameEntity>>(json, framesType).orEmpty()
}
