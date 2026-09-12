package vn.petpomodoro.game

/**
 * Catalog.kt — Bộ sinh vật data-driven (spec §1.4, §2.1–2.3).
 * Thêm loài mới = thêm 1 entry ở [Catalog.lines] — KHÔNG sửa core.
 * Tên 100% khớp spec §2.1–2.3.
 */
data class StageDef(
    val atLevel: Int,
    val id: String,
    val name: String,
    val desc: String,
    /** Tên drawable (drawable-nodpi) — sprite v3 tách từ ảnh mẫu user (direction-approved.md) */
    val sprite: String,
)

data class BranchDef(
    val id: String,
    val name: String,
    val short: String,
    val meaning: String,
    /** Hậu tố form Stage 3: "Diễm Long" + " Hiền" = "Diễm Long Hiền" */
    val formSuffix: String,
    val cond: Condition,
)

data class HybridDef(
    val id: String,
    val name: String,
    /** 2 line nguyên tố tạo form lai (VD: Lửa × Thủy) */
    val from: List<String>,
    val cond: Condition,
)

data class SpeciesDef(
    val id: String,
    val element: String,
    val name: String,
    /** Màu chủ đạo của line (Memphis palette, dùng cho card trứng / chip) */
    val colorArgb: Long,
    val stages: List<StageDef>,
    val branches: List<BranchDef>,
    val hybridsWith: List<String>,
) {
    fun stageFor(level: Int): StageDef = stages.last { level >= it.atLevel }
    fun nextStage(level: Int): StageDef? = stages.firstOrNull { it.atLevel > level }
    fun stageById(id: String): StageDef? = stages.firstOrNull { it.id == id }
}

object Catalog {

    const val LEVEL_STAGE_2 = 8
    const val LEVEL_STAGE_3 = 16

    val lines: List<SpeciesDef> = listOf(
        SpeciesDef(
            id = "flame", element = "🔥", name = "Lửa", colorArgb = 0xFFBC3A28,
            stages = listOf(
                StageDef(1, "tan-lua", "Tàn Lửa", "hổ lửa con", "sprite_tan_lua"),
                StageDef(LEVEL_STAGE_2, "bong-bot", "Bồng Bột", "thiếu niên lửa · mọc cánh mầm", "sprite_bong_bot"),
                StageDef(LEVEL_STAGE_3, "diem-long", "Diễm Long", "rồng lửa nhỏ", "sprite_diem_long"),
            ),
            branches = listOf(
                BranchDef(
                    id = "hien", name = "Hiền", short = "streak ≥ 7",
                    meaning = "aura dịu — phần thưởng thói quen đều đặn", formSuffix = "Hiền",
                    cond = Condition.And(
                        listOf(Condition.LevelAtLeast(LEVEL_STAGE_3), Condition.StreakAtLeast(7))
                    ),
                ),
                BranchDef(
                    id = "chien-binh", name = "Chiến Binh", short = "Sức mạnh ≥ 80",
                    meaning = "giáp, uy phong — phần thưởng sức mạnh cao", formSuffix = "Chiến Binh",
                    cond = Condition.And(
                        listOf(Condition.LevelAtLeast(LEVEL_STAGE_3), Condition.PowerAtLeast(80))
                    ),
                ),
            ),
            hybridsWith = listOf("water", "grass"),
        ),
        SpeciesDef(
            id = "water", element = "💧", name = "Thủy", colorArgb = 0xFF2B54A3,
            stages = listOf(
                StageDef(1, "giot", "Giọt", "gấu nước", "sprite_giot"),
                StageDef(LEVEL_STAGE_2, "suoi-vot", "Suối Vọt", "cá heo nước ngọt", "sprite_suoi_vot"),
                StageDef(LEVEL_STAGE_3, "trieu-long", "Triều Long", "rồng sóng", "sprite_trieu_long"),
            ),
            branches = listOf(
                BranchDef(
                    id = "hien", name = "Hiền", short = "streak ≥ 7",
                    meaning = "thói quen đều đặn", formSuffix = "Hiền",
                    cond = Condition.And(
                        listOf(Condition.LevelAtLeast(LEVEL_STAGE_3), Condition.StreakAtLeast(7))
                    ),
                ),
                BranchDef(
                    id = "chien-binh", name = "Chiến Binh", short = "Sức mạnh ≥ 80",
                    meaning = "sức mạnh cao", formSuffix = "Chiến Binh",
                    cond = Condition.And(
                        listOf(Condition.LevelAtLeast(LEVEL_STAGE_3), Condition.PowerAtLeast(80))
                    ),
                ),
            ),
            hybridsWith = listOf("flame", "grass"),
        ),
        SpeciesDef(
            id = "grass", element = "🌿", name = "Thảo", colorArgb = 0xFF5F6B2F,
            stages = listOf(
                StageDef(1, "mam", "Mầm", "hạt nảy mầm", "sprite_mam"),
                StageDef(LEVEL_STAGE_2, "bup-xanh", "Búp Xanh", "hươu lá", "sprite_bup_xanh"),
                StageDef(LEVEL_STAGE_3, "co-thu-linh", "Cổ Thụ Linh", "linh thú cây cổ tích", "sprite_co_thu_linh"),
            ),
            branches = listOf(
                BranchDef(
                    id = "hien", name = "Hiền", short = "streak ≥ 7",
                    meaning = "thói quen đều đặn", formSuffix = "Hiền",
                    cond = Condition.And(
                        listOf(Condition.LevelAtLeast(LEVEL_STAGE_3), Condition.StreakAtLeast(7))
                    ),
                ),
                BranchDef(
                    id = "chien-binh", name = "Chiến Binh", short = "Sức mạnh ≥ 80",
                    meaning = "sức mạnh cao", formSuffix = "Chiến Binh",
                    cond = Condition.And(
                        listOf(Condition.LevelAtLeast(LEVEL_STAGE_3), Condition.PowerAtLeast(80))
                    ),
                ),
            ),
            hybridsWith = listOf("flame", "water"),
        ),
    )

    /** §2.3 Form lai hiếm — điều kiện KÉP `and[power≥85, streak≥10]` */
    val hybrids: List<HybridDef> = listOf(
        HybridDef("hoi-nuoc-boc", "Hơi Nước Bốc", listOf("flame", "water"),
            Condition.And(listOf(Condition.PowerAtLeast(85), Condition.StreakAtLeast(10)))),
        HybridDef("diem-hoa", "Diễm Hoa", listOf("grass", "flame"),
            Condition.And(listOf(Condition.PowerAtLeast(85), Condition.StreakAtLeast(10)))),
        HybridDef("sen-mua", "Sen Mưa", listOf("water", "grass"),
            Condition.And(listOf(Condition.PowerAtLeast(85), Condition.StreakAtLeast(10)))),
    )

    fun line(id: String): SpeciesDef = lines.first { it.id == id }

    fun lineBySpeciesStage(stageId: String): SpeciesDef =
        lines.first { it.stageById(stageId) != null }

    /** Nhánh Stage 3: chọn NHÁNH ĐẦU TIÊN thỏa điều kiện; không nhánh nào → form thường (null). */
    fun pickBranch(line: SpeciesDef, s: GameState): BranchDef? =
        line.branches.firstOrNull { it.cond.evaluate(s) }

    /** Các form lai liên quan đến line đang nuôi (để panel "Lai tạo" trên màn Tiến hoá) */
    fun hybridsFor(lineId: String): List<HybridDef> =
        hybrids.filter { lineId in it.from }
}
