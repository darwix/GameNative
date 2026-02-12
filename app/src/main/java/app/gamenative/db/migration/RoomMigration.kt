package app.gamenative.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

private const val DROP_TABLE = "DROP TABLE IF EXISTS " // Trailing Space

internal val ROOM_MIGRATION_V7_to_V8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        // Dec 5, 2025: Friends and Chat features removed
        connection.execSQL(DROP_TABLE + "chat_message")
        connection.execSQL(DROP_TABLE + "emoticon")
        connection.execSQL(DROP_TABLE + "steam_friend")
    }
}

internal val ROOM_MIGRATION_V12_to_V13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        val existingColumns = mutableSetOf<String>()
        connection.prepare("PRAGMA table_info(`epic_games`)").use { statement ->
            while (statement.step()) {
                existingColumns.add(statement.getText(1))
            }
        }

        val columnsToAdd = mapOf(
            "deployment_id" to "TEXT NOT NULL DEFAULT ''",
            "product_id" to "TEXT NOT NULL DEFAULT ''",
            "application_id" to "TEXT NOT NULL DEFAULT ''",
            "additional_command_line" to "TEXT NOT NULL DEFAULT ''"
        )

        for ((columnName, columnDef) in columnsToAdd) {
            if (!existingColumns.contains(columnName)) {
                connection.execSQL("ALTER TABLE `epic_games` ADD COLUMN `$columnName` $columnDef")
            }
        }
    }
}
