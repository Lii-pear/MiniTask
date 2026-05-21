package com.example.minitask.domain.routine

enum class RoutineType(val storageValue: String) {
    DAILY("DAILY"),
    INTERVAL("INTERVAL"),
    WEEKLY("WEEKLY");

    companion object {
        fun fromStorage(value: String?): RoutineType {
            return entries.firstOrNull { type ->
                type.storageValue.equals(value, ignoreCase = true) ||
                    type.name.equals(value, ignoreCase = true)
            } ?: DAILY
        }
    }
}

data class RoutineScheduleInput(
    val type: RoutineType,
    val intervalDays: Int = 1,
    val weekDays: Set<Int> = emptySet()
) {
    val storageType: String = type.storageValue

    val storageRepeatValue: String = when (type) {
        RoutineType.DAILY -> ""
        RoutineType.INTERVAL -> intervalDays.coerceAtLeast(1).toString()
        RoutineType.WEEKLY -> weekDays
            .filter { it in 1..7 }
            .distinct()
            .sorted()
            .joinToString(",")
    }

    companion object {
        fun daily() = RoutineScheduleInput(type = RoutineType.DAILY)

        fun interval(days: Int) = RoutineScheduleInput(
            type = RoutineType.INTERVAL,
            intervalDays = days.coerceAtLeast(1)
        )

        fun weekly(days: Iterable<Int>) = RoutineScheduleInput(
            type = RoutineType.WEEKLY,
            weekDays = days.filter { it in 1..7 }.toSet()
        )
    }
}
