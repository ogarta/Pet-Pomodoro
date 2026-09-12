package vn.petpomodoro.game

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Backup.kt — xuất/nhập bản lưu pet dạng JSON, CÙNG SHAPE với bên web
 * (`SerializedDocs`: state/pet, state/meta, state/settings, systems/…).
 * Mục tiêu: chống mất dữ liệu + dùng chéo 2 nền tảng.
 *
 * Quy ước:
 *  - file dùng id loài của WEB (lua/thuy/thao) — Android map flame/water/grass khi xuất/nhập;
 *    stageId và branchId trùng nhau nên giữ nguyên.
 *  - history lưu theo shape web (date "YYYY-MM-DD"); sessionLog là phần riêng Android,
 *    web import sẽ bỏ qua (bên web không có).
 *  - trường nào nền tảng kia không có → để nguyên/opt; validator mỗi bên tự bỏ qua phần thừa.
 */
object Backup {

    private val SPECIES_TO_WEB = mapOf("flame" to "lua", "water" to "thuy", "grass" to "thao")
    private val WEB_TO_SPECIES = SPECIES_TO_WEB.entries.associate { (k, v) -> v to k }

    data class Imported(val state: GameState, val alerts: AlertSettings)

    // ── xuất ────────────────────────────────────────────────────────────

    fun exportJson(state: GameState, alerts: AlertSettings, timer: SavedTimer?): String {
        val root = JSONObject()

        root.put("state/pet", JSONObject().apply {
            put("speciesId", SPECIES_TO_WEB[state.speciesId] ?: state.speciesId)
            put("stageId", state.stageId)
            put("level", state.level)
            put("xp", state.xp)
            put("condition", state.condition)
            put("coins", state.coins)
            put("streak", state.streak)
            put("todaySessions", state.todaySessions)
            put("todayMinutes", state.todayMinutes)
            put("todayXp", state.todayXp)
            put("dayFed", state.dayFed)
            put("dayHadSession", state.dayHadSession)
            put("lastDayEpochDay", state.lastDayEpochDay)
        })

        root.put("state/meta", JSONObject().apply {
            put("today", epochDayToKey(state.lastDayEpochDay))
            put("lastActiveDate", epochDayToKey(state.lastDayEpochDay))
            put("focusStreak", 0) // bên web là chuỗi phiên liên tục — Android không track, để 0
            put("timer", timer?.let { t ->
                JSONObject().apply {
                    put("phase", t.phase)
                    put("running", t.running)
                    put("remainingMs", t.remainingMs)
                    put("endAt", if (t.running) t.endAtWallMs else null)
                    put("totalMs", t.totalMs)
                }
            } ?: JSONObject.NULL)
            put("alerts", JSONObject().apply {
                put("sound", alerts.sound)
                put("notify", alerts.notify)
            })
        })

        root.put("state/settings", JSONObject().apply {
            put("focusMinutes", Formulas.FOCUS_MINUTES)
            put("breakMinutes", Formulas.BREAK_MINUTES)
        })

        root.put("systems/power", JSONObject().apply { put("value", state.power) })

        root.put("systems/evolution", JSONObject().apply {
            put("unlocked", JSONArray(state.unlockedFormIds.toList()))
            put("branchId", state.branchId ?: JSONObject.NULL)
        })

        root.put("systems/stats", JSONObject().apply {
            put("healthyDays", state.healthyDays)
            put("history", JSONArray().apply {
                state.history.forEach { h ->
                    put(JSONObject().apply {
                        put("date", epochDayToKey(h.epochDay))
                        put("sessions", h.sessions)
                        put("minutes", h.minutes)
                        put("xp", 0)
                        put("goal", h.goalMet)
                    })
                }
            })
            put("sessionLog", JSONArray().apply {
                state.sessionLog.forEach { s ->
                    put(JSONObject().apply {
                        put("epochDay", s.epochDay)
                        put("minuteOfDay", s.minuteOfDay)
                        put("minutes", s.minutes)
                        put("xp", s.xp)
                    })
                }
            })
        })

        return root.toString(2)
    }

    // ── nhập ────────────────────────────────────────────────────────────

    /** null = file không hợp lệ (không đụng state hiện tại). */
    fun parseJson(text: String): Imported? = try {
        val root = JSONObject(text)
        val pet = root.optJSONObject("state/pet") ?: return null

        val speciesWeb = pet.optString("speciesId", "")
        val species = WEB_TO_SPECIES[speciesWeb]
            ?: speciesWeb.takeIf { it in WEB_TO_SPECIES.values }
            ?: return null
        val line = Catalog.line(species)
        val stage = line.stageById(pet.optString("stageId", "")) ?: line.stages.first()

        val meta = root.optJSONObject("state/meta")
        val alertsRaw = meta?.optJSONObject("alerts")
        val stats = root.optJSONObject("systems/stats")
        val evo = root.optJSONObject("systems/evolution")

        val history = mutableListOf<DayRecord>()
        stats?.optJSONArray("history")?.let { arr ->
            for (i in 0 until arr.length()) {
                val h = arr.getJSONObject(i)
                val ed = keyToEpochDay(h.optString("date", "")).takeIf { it > 0 }
                    ?: h.optLong("epochDay", 0L)
                if (ed > 0) {
                    history += DayRecord(ed, h.optInt("minutes"), h.optInt("sessions"), h.optBoolean("goal"))
                }
            }
        }

        val sessionLog = mutableListOf<SessionLogEntry>()
        stats?.optJSONArray("sessionLog")?.let { arr ->
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                sessionLog += SessionLogEntry(
                    s.optLong("epochDay"), s.optInt("minuteOfDay"), s.optInt("minutes"), s.optInt("xp"),
                )
            }
        }

        val branchRaw = evo?.optString("branchId", "").orEmpty()
        val unlocked = mutableSetOf(stage.id)
        evo?.optJSONArray("unlocked")?.let { arr ->
            for (i in 0 until arr.length()) unlocked.add(arr.getString(i))
        }

        val importedLastDay = pet.optLong("lastDayEpochDay", 0L)
            .takeIf { it > 0 }
            ?: meta?.optString("today", "")?.let { keyToEpochDay(it) }?.takeIf { it > 0 }
            ?: LocalDate.now().toEpochDay()

        val state = GameState(
            speciesId = species,
            stageId = stage.id,
            branchId = branchRaw.takeIf { it.isNotEmpty() && it != "null" },
            level = pet.optInt("level", 1).coerceAtLeast(1),
            xp = pet.optInt("xp", 0).coerceAtLeast(0),
            power = root.optJSONObject("systems/power")?.optInt("value", 40)?.coerceIn(0, 100) ?: 40,
            condition = pet.optInt("condition", 80).coerceIn(0, 100),
            coins = pet.optInt("coins", Formulas.SUSHI_START).coerceAtLeast(0),
            streak = pet.optInt("streak", 0).coerceAtLeast(0),
            healthyDays = (stats?.optInt("healthyDays") ?: 0).coerceAtLeast(0),
            todaySessions = pet.optInt("todaySessions", 0).coerceAtLeast(0),
            todayMinutes = pet.optInt("todayMinutes", 0).coerceAtLeast(0),
            todayXp = pet.optInt("todayXp", 0).coerceAtLeast(0),
            dayFed = pet.optBoolean("dayFed", false),
            dayHadSession = pet.optBoolean("dayHadSession", false),
            lastDayEpochDay = importedLastDay,
            unlockedFormIds = unlocked,
            history = history,
            sessionLog = sessionLog,
        )
        Imported(
            state,
            AlertSettings(
                sound = alertsRaw?.optBoolean("sound") ?: true,
                notify = alertsRaw?.optBoolean("notify") ?: true,
            ),
        )
    } catch (_: Exception) {
        null
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private fun epochDayToKey(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).toString() // ISO yyyy-MM-dd

    private fun keyToEpochDay(key: String): Long = try {
        if (key.isEmpty()) 0L else LocalDate.parse(key).toEpochDay()
    } catch (_: Exception) {
        0L
    }
}
