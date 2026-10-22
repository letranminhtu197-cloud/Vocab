package com.vocab.ielts.offline

/** Tiến độ thành tựu được tính hoàn toàn từ dữ liệu cục bộ, không cần lưu thêm bảng database. */
data class AchievementProgress(
    val id: String,
    val title: String,
    val description: String,
    val current: Int,
    val target: Int,
    val unit: String
) {
    val unlocked: Boolean get() = current >= target
    val fraction: Float get() = (current.coerceAtLeast(0).toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val displayedCurrent: Int get() = current.coerceAtLeast(0).coerceAtMost(target)
}

object AchievementLogic {
    /** Tạo danh sách thành tựu từ thống kê hiện có; số liệu không âm và mục tiêu ngày luôn hợp lệ. */
    fun evaluate(
        totalWords: Int,
        totalPracticeEvents: Int,
        activeStreak: Int,
        todayActivities: Int,
        dailyGoal: Int
    ): List<AchievementProgress> {
        val safeGoal = StudyGoalLogic.normalizeGoal(dailyGoal)
        return listOf(
            AchievementProgress("first_word", "Từ đầu tiên", "Tạo mục từ đầu tiên trong sổ riêng.", totalWords, 1, "từ"),
            AchievementProgress("first_review", "Bắt đầu ghi nhớ", "Hoàn thành lượt học hoặc ôn đầu tiên.", totalPracticeEvents, 1, "lượt"),
            AchievementProgress("word_collector", "Kho từ 50", "Tự xây dựng sổ từ với ít nhất 50 mục.", totalWords, 50, "từ"),
            AchievementProgress("practice_100", "100 lượt luyện", "Tích lũy 100 lượt học và ôn tập.", totalPracticeEvents, 100, "lượt"),
            AchievementProgress("streak_7", "Bền bỉ 7 ngày", "Duy trì chuỗi học liên tiếp trong 7 ngày.", activeStreak, 7, "ngày"),
            AchievementProgress("daily_goal", "Mục tiêu hôm nay", "Hoàn thành mục tiêu học và ôn trong ngày.", todayActivities, safeGoal, "lượt")
        )
    }
}
