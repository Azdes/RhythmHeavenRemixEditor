package chrislo27.rhre.registry

import ionium.templates.Main
import ionium.util.i18n.Localization
import org.luaj.vm2.LuaValue

object SeriesList {
    @JvmField val list: List<Series> = mutableListOf(Series("other", true),
                                                     Series("tengoku", true),
                                                     Series("ds", true),
                                                     Series("fever", true),
                                                     Series("megamix", true),
                                                     Series("side", true),
                                                     Series("custom", false, hasLocalizedName = true))
    var map: Map<String, Series> = list.associateBy(Series::name)
        private set

    val CUSTOM: Series = map["custom"]!!
    val UNKNOWN: Series = map["other"]!!
    @JvmField val TENGOKU: Series = map["tengoku"]!!

    operator fun get(key: String): Series? {
        return map[key]
    }

    fun getOrPut(key: String): Series {
        val ret = this[key]
        if (ret == null) {
            addSeries(Series(key))
        }

        return this[key]!!
    }

    fun update() {
        map = list.associateBy(Series::name)
    }

    fun addSeries(s: Series) {
        list as MutableList
        list.add(s)

        Main.logger.info("Added a new series: " + s)

        update()
    }
}

data class Series(val name: String, val builtIn: Boolean = false, val hasLocalizedName: Boolean = builtIn) {

    private val idToName: String by lazy {
        name.replace("_", " ")
    }

    fun getLocalizedName(): String = if (hasLocalizedName) Localization.get("series." + name) else idToName

    val luaValue: LuaValue by lazy {
        LuaValue.valueOf(name)
    }

}