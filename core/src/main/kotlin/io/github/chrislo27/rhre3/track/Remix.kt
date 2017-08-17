package io.github.chrislo27.rhre3.track

import com.badlogic.gdx.graphics.OrthographicCamera
import io.github.chrislo27.rhre3.RHRE3Application
import io.github.chrislo27.rhre3.editor.Editor
import io.github.chrislo27.rhre3.entity.EndEntity
import io.github.chrislo27.rhre3.entity.Entity
import io.github.chrislo27.rhre3.oopsies.ActionHistory
import io.github.chrislo27.rhre3.registry.GameRegistry
import io.github.chrislo27.rhre3.registry.datamodel.impl.Cue
import io.github.chrislo27.rhre3.tempo.Tempos
import io.github.chrislo27.rhre3.track.music.MusicData
import io.github.chrislo27.rhre3.track.music.MusicVolumes
import io.github.chrislo27.rhre3.track.timesignature.TimeSignatures
import io.github.chrislo27.rhre3.tracker.TrackerContainer
import io.github.chrislo27.toolboks.lazysound.LazySound
import io.github.chrislo27.toolboks.registry.AssetRegistry


class Remix(val camera: OrthographicCamera, val editor: Editor) : ActionHistory<Remix>() {

    val main: RHRE3Application
        get() = editor.main

    val entities: MutableList<Entity> = mutableListOf()
    val trackers: MutableList<TrackerContainer<*>> = mutableListOf()
    val timeSignatures: TimeSignatures = run {
        val ts = TimeSignatures()
        trackers += ts
        ts
    }
    val musicVolumes: MusicVolumes = run {
        val mv = MusicVolumes()
        trackers += mv
        mv
    }
    val tempos: Tempos = run {
        val t = Tempos()
        trackers += t
        t
    }

    var seconds: Float = 0f
        set(value) {
            field = value
            beat = tempos.secondsToBeats(field)
        }
    var beat: Float = 0f
        private set

    var playbackStart: Float = 0f
    var musicStartSec: Float = 0f
    var music: MusicData? = null
    private var lastMusicPosition: Float = -1f
    var metronome: Boolean = false
        set(value) {
            field = value
            lastTickBeat = beat.toInt()
        }
    private var lastTickBeat = Int.MIN_VALUE

    private val metronomeSFX: List<LazySound> by lazy {
        listOf(
                (GameRegistry.data.objectMap["countInEn/cowbell"] as? Cue)?.sound ?:
                        throw RuntimeException("Missing metronome sound")
              )
    }

    var playState: PlayState = PlayState.STOPPED
        set(value) {
            val old = field
            val music = music
            field = value
            when (field) {
                PlayState.STOPPED -> {
                    AssetRegistry.stopAllSounds()
                    music?.music?.stop()
                }
                PlayState.PAUSED -> {
                    AssetRegistry.pauseAllSounds()
                    music?.music?.pause()
                }
                PlayState.PLAYING -> {
                    lastMusicPosition = -1f
                    AssetRegistry.resumeAllSounds()
                    if (old == PlayState.STOPPED) {
                        seconds = tempos.beatsToSeconds(playbackStart)
                        entities.forEach {
                            if (it.bounds.x + it.bounds.width < beat) {
                                it.playbackCompletion = PlaybackCompletion.FINISHED
                            } else {
                                it.playbackCompletion = PlaybackCompletion.WAITING
                            }
                        }

                        lastTickBeat = Math.ceil(playbackStart - 1.0).toInt()
                    }
                    if (music != null) {
                        music.music.play()
                        setMusicVolume()
                        music.music.position = seconds - musicStartSec
                    }
                }
            }
        }

    private fun setMusicVolume() {
        music?.music?.volume = musicVolumes.getPercentageVolume(beat)
    }

    init {
//        musicVolumes.add(MusicVolumeChange(1f, 95))
//        musicVolumes.add(MusicVolumeChange(3f, 86))
//        tempos.add(TempoChange(0f, 128f))
//        tempos.add(TempoChange(3f, 192f))
//        timeSignatures.add(TimeSignature(0, 2))
//        timeSignatures.add(TimeSignature(2, 3))
//        timeSignatures.add(TimeSignature(5, 4))
    }

    fun getLastPoint(): Float {
        if (entities.isEmpty())
            return 0f
        return if (entities.isNotEmpty() && entities.any { it is EndEntity }) {
            entities.first { it is EndEntity }.bounds.x
        } else {
            val last = entities.maxBy { it.bounds.x + it.bounds.width }!!
            last.bounds.x + last.bounds.y
        }
    }

    fun entityUpdate(entity: Entity) {
        if (entity.playbackCompletion == PlaybackCompletion.WAITING) {
            if (beat in entity.bounds.x..(entity.bounds.x + entity.bounds.width)) {
                entity.playbackCompletion = PlaybackCompletion.PLAYING
                entity.onStart()
            }
        }

        if (entity.playbackCompletion == PlaybackCompletion.PLAYING) {
            entity.whilePlaying()
            if (entity.isFinished()) {
                entity.playbackCompletion = PlaybackCompletion.FINISHED
                entity.onEnd()
            }
        }
    }

    fun timeUpdate(delta: Float) {
        if (playState != PlayState.PLAYING)
            return

        val music: MusicData? = music

        seconds += delta
        if (music != null) {
            val oldPosition = lastMusicPosition
            val position = music.music.position
            lastMusicPosition = position

            if (oldPosition != position) {
                seconds = position
            }

            val musicVolume = musicVolumes.getPercentageVolume(beat)
            if (musicVolume != music.music.volume) {
                music.music.volume = musicVolume
            }
        }

        entities.forEach { entity ->
            if (entity.playbackCompletion != PlaybackCompletion.FINISHED) {
                entityUpdate(entity)
            }
        }

        if (Math.floor(beat.toDouble()) > lastTickBeat) {
            lastTickBeat = Math.floor(beat.toDouble()).toInt()
            if (metronome) {
                val isStartOfMeasure = timeSignatures.getMeasurePart(lastTickBeat.toFloat()) == 0
                metronomeSFX[Math.round(Math.abs(beat)) % metronomeSFX.size].sound.play(1f,
                                                                                        if (isStartOfMeasure) 1.5f else 1.1f,
                                                                                        0f)
            }
        }
    }

}