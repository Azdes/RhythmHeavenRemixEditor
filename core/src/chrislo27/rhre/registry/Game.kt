package chrislo27.rhre.registry

import com.badlogic.gdx.graphics.Texture
import ionium.registry.AssetRegistry
import ionium.util.MathHelper
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua


data class Game(val id: String, val name: String, val soundCues: List<SoundCue>,
                val patterns: List<Pattern>, val series: Series, val icon: String?,
                val iconIsRawPath: Boolean = false, val notRealGame: Boolean = false,
                val priority: Int = 0) : Comparable<Game> {

    override fun compareTo(other: Game): Int {
        if (other.priority == this.priority) {
            return this.id.compareTo(other.id)
        } else {
            return other.priority - this.priority
        }
    }

    private companion object {
        val defaultPointerString: String = "➡"
        val levels: List<String> = "▁▂▃▄▅▆▇█".reversed().map(Character::toString)
        val bts: List<String> = "❶❷❸❹❸❷".map(Character::toString)
    }

    val pointerString: String get() {
        return when (id) {
            "extraSFX" -> "★"
            "rhythmTweezers", "rhythmTweezersGba" -> "✂"
            "moaiDooWop" -> "‼"
            "builtToScaleFever" -> {
                val time = MathHelper.getSawtoothWave(0.5f * bts.size)

                bts[(bts.size * time).toInt().coerceIn(0, bts.size - 1)]
            }
            "fruitBasket" -> {
                val time = MathHelper.getTriangleWave(1.5f)

                levels[((levels.size * (time * 8) - 6).toInt() + 1).coerceIn(0, levels.size - 1)]
            }
            "clapTrap" -> "〠"
            else -> defaultPointerString
        }
    }

    private val iconTextureID: String by lazy { "gameIcon_$id" }
    val iconTexture: Texture get() {
        return AssetRegistry.getTexture(iconTextureID)
    }

    fun isCustom() = series == SeriesList.CUSTOM

    fun getPattern(id: String): Pattern? {
        return patterns.find { it.id == "${this.id}_$id" }
    }

    fun getCue(id: String): SoundCue? {
        return soundCues.find { it.id == "${this.id}/$id" }
    }

    val luaValue: LuaValue by lazy {
        val l = LuaValue.tableOf()

        l.set("id", id)
        l.set("name", name)
        l.set("series", series.luaValue)
        l.set("cues", LuaValue.listOf(soundCues.map { it.luaValue }.toTypedArray()))
        l.set("patterns", LuaValue.listOf(patterns.map { it.luaValue }.toTypedArray()))
        l.set("priority", priority)
        l.set("isRealGame", CoerceJavaToLua.coerce(!notRealGame))

        return@lazy l
    }

}
