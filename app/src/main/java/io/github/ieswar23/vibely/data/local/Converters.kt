package io.github.ieswar23.vibely.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.ieswar23.vibely.data.local.entity.PollOptionEntity
import io.github.ieswar23.vibely.data.local.entity.StoryFrameEntity

class Converters {
    private val gson = Gson()
    private val framesType = object : TypeToken<List<StoryFrameEntity>>() {}.type
    private val pollOptionsType = object : TypeToken<List<PollOptionEntity>>() {}.type

    @TypeConverter
    fun framesToJson(frames: List<StoryFrameEntity>): String = gson.toJson(frames)

    @TypeConverter
    fun jsonToFrames(json: String): List<StoryFrameEntity> =
        gson.fromJson<List<StoryFrameEntity>>(json, framesType).orEmpty()

    @TypeConverter
    fun pollOptionsToJson(options: List<PollOptionEntity>): String = gson.toJson(options)

    @TypeConverter
    fun jsonToPollOptions(json: String): List<PollOptionEntity> =
        gson.fromJson<List<PollOptionEntity>>(json, pollOptionsType).orEmpty()
}
