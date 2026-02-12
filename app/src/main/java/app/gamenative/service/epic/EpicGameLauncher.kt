package app.gamenative.service.epic

import android.content.Context
import app.gamenative.data.EpicGame
import app.gamenative.data.EpicGameToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Helper functionality for launching Epic Games with correct execution params for online verification
 *
 * Handles:
 * - Getting authentication tokens before launch
 * - Building Epic Games Services command-line parameters
 * - Managing ownership token files for DRM-protected games
 */
object EpicGameLauncher {

    data class EpicLaunchInfo(
        val parameters: List<String>,
        val envVars: Map<String, String> = emptyMap()
    )

    /**
     * Build launch parameters for an Epic game
     *
     * Returns a list of command-line arguments to pass to the game executable
     * for Epic Games Services authentication
     *
    */
    suspend fun buildLaunchParameters(
        context: Context,
        game: EpicGame,
        offline: Boolean = false,
        languageCode: String = "en-US"
    ): Result<EpicLaunchInfo> {
        return try {
            val params = mutableListOf<String>()
            val envVars = mutableMapOf<String, String>()

            // Do offline play if offline.
            if (offline) {
                if (game.canRunOffline) {
                    Timber.tag("EPIC").i("Launching ${game.appName} in offline mode (no authentication)")
                    return Result.success(EpicLaunchInfo(params, envVars))
                } else {
                    Timber.tag("EPIC").w("${game.appName} cannot run offline, will attempt online launch")
                }
            }

            Timber.tag("EPIC").d("Launching ${game.appName} online, getting game launch token...")

            val tokenResult = EpicAuthManager.getGameLaunchToken(
                context = context,
                namespace = game.namespace,
                catalogItemId = game.catalogId,
                requiresOwnershipToken = game.requiresOT
            )

            if (tokenResult.isFailure) {
                return Result.failure(tokenResult.exceptionOrNull() ?: Exception("Failed to get launch token"))
            }

            val gameToken: EpicGameToken? = tokenResult.getOrNull()

            if (gameToken == null) {
                Timber.tag("EPIC").w("Game Token is null for ${game.appName}")
                return Result.failure(Exception("Game token is null for ${game.appName}"))
            }

            Timber.tag("EPIC").d("Got Game Token for ${game.appName}")

            // Save ownership token to temp file if present
            val ownershipTokenPath = if (gameToken.ownershipToken != null) {
                saveOwnershipTokenToFile(context, game.namespace, game.catalogId, gameToken.ownershipToken)
            } else {
                null
            }

            Timber.tag("EPIC").i("Game launch token obtained for ${game.appName}")

            // Authentication parameters
            params.add("-AUTH_LOGIN=unused")
            params.add("-AUTH_PASSWORD=${gameToken.authCode}")
            params.add("-AUTH_TYPE=exchangecode")
            params.add("-epicapp=${game.appName}")
            params.add("-epicenv=Prod")
            params.add("-epiceosenv=prod")
            params.add("-EABackend=prod")

            // Epic Portal flag
            params.add("-EpicPortal")

            // User information parameters
            val displayName = gameToken.displayName
            val accountId = gameToken.accountId

            params.add("-epicusername=$displayName")
            params.add("-epicuserid=$accountId")
            params.add("-epiclocale=$languageCode")
            params.add("-epicsandboxid=${game.namespace}")

            // Add deploymentId if available
            if (game.deploymentId.isNotEmpty()) {
                params.add("-epicdeploymentid=${game.deploymentId}")
            }

            // Ownership token for DRM-protected games
            if (ownershipTokenPath != null) {
                params.add("-epicovt=$ownershipTokenPath")
                Timber.tag("EPIC").d("Added ownership token path: $ownershipTokenPath")
            }

            // Additional command-line parameters from game metadata
            if (game.additionalCommandLine.isNotEmpty()) {
                Timber.tag("EPIC").i("Adding additional command line: ${game.additionalCommandLine}")
                // Simple split by space, but Legendary handles this more robustly.
                // For now, just add them as separate parameters if they start with '-'
                game.additionalCommandLine.split(" ").forEach {
                    if (it.isNotEmpty()) {
                        params.add(it)
                    }
                }
            }

            // Set environment variables for EOS
            envVars["EOS_LOGIN_CREDENTIALS"] = "EXCHANGE_CODE:${gameToken.authCode}"
            // Inform EOS SDK that it's being launched from a launcher to avoid restart loops
            envVars["EOS_PLATFORM_CHECKFORLAUNCHERANDRESTART_ENV_VAR"] = "1"

            Timber.tag("EPIC").d("Built ${params.size} launch parameters for ${game.appName}")
            Result.success(EpicLaunchInfo(params, envVars))
        } catch (e: Exception) {
            Timber.e(e, "Failed to build launch parameters")
            Result.failure(e)
        }
    }

    /**
     * Save ownership token bytes to temp file
     * File path format: {temp_dir}/{namespace}{catalogItemId}.ovt
     *
     * @return Absolute path to the saved token file
     * @throws IllegalArgumentException if ownershipTokenHex is invalid
     * @throws IOException if file write fails
     */
    private fun saveOwnershipTokenToFile(
        context: Context,
        namespace: String,
        catalogItemId: String,
        ownershipTokenHex: String
    ): String {
        // Validate hex string
        if (ownershipTokenHex.isEmpty()) {
            throw IllegalArgumentException("Ownership token hex string is empty")
        }
        if (ownershipTokenHex.length % 2 != 0) {
            throw IllegalArgumentException("Ownership token hex string has odd length: ${ownershipTokenHex.length}")
        }
        if (!ownershipTokenHex.matches(Regex("^[0-9A-Fa-f]+$"))) {
            throw IllegalArgumentException("Ownership token hex string contains invalid characters")
        }

        val tempDir = File(context.cacheDir, "epic_tokens")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        // Sanitize namespace and catalogItemId to prevent path traversal
        val sanitizedNamespace = namespace.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val sanitizedCatalogId = catalogItemId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val tokenFile = File(tempDir, "$sanitizedNamespace$sanitizedCatalogId.ovt")

        try {
            // Convert hex string back to bytes
            val tokenBytes = ownershipTokenHex.chunked(2)
                .map { hexByte ->
                    try {
                        hexByte.toInt(16).toByte()
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException("Invalid hex byte: $hexByte", e)
                    }
                }
                .toByteArray()

            tokenFile.writeBytes(tokenBytes)
            Timber.tag("EPIC").d("Ownership token saved to: ${tokenFile.absolutePath}")
            return tokenFile.absolutePath
        } catch (e: IllegalArgumentException) {
            Timber.tag("EPIC").e(e, "Failed to parse ownership token hex string")
            throw e
        } catch (e: IOException) {
            Timber.tag("EPIC").e(e, "Failed to write ownership token file: ${tokenFile.absolutePath}")
            throw e
        }
    }

    /**
     * Create .egstore folder with manifest and mancpn files
     * This helps games verify they are being launched from a legitimate launcher
     */
    suspend fun createEgStore(
        context: Context,
        game: EpicGame
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (game.installPath.isEmpty()) {
                return@withContext Result.failure(Exception("Install path is empty"))
            }

            val egStoreDir = File(game.installPath, ".egstore")
            if (!egStoreDir.exists()) {
                egStoreDir.mkdirs()
            }

            // Fetch manifest from Epic to get latest version and binary data
            val epicManager = EpicService.getInstance()?.epicManager
                ?: return@withContext Result.failure(Exception("EpicManager not available"))

            val manifestResult = epicManager.fetchManifestFromEpic(
                context,
                game.namespace,
                game.catalogId,
                game.appName
            )

            if (manifestResult.isFailure) {
                return@withContext Result.failure(manifestResult.exceptionOrNull() ?: Exception("Failed to fetch manifest"))
            }

            val manifestData = manifestResult.getOrNull()!!

            // Update deploymentId in database if it was missing or changed
            if (manifestData.deploymentId != null && manifestData.deploymentId != game.deploymentId) {
                Timber.tag("EPIC").i("Updating deploymentId for ${game.appName}: ${manifestData.deploymentId}")
                epicManager.updateGame(game.copy(deploymentId = manifestData.deploymentId))
            }

            // Consistent GUID based on appName
            val installationGuid = UUID.nameUUIDFromBytes(game.appName.toByteArray()).toString().replace("-", "").uppercase()

            // Write .manifest file
            val manifestFile = File(egStoreDir, "$installationGuid.manifest")
            manifestFile.writeBytes(manifestData.manifestBytes)

            // Write .mancpn file (JSON format)
            val mancpn = JSONObject().apply {
                put("FormatVersion", 0)
                put("AppName", game.appName)
                put("CatalogItemId", game.catalogId)
                put("CatalogNamespace", game.namespace)
            }
            val mancpnFile = File(egStoreDir, "$installationGuid.mancpn")
            mancpnFile.writeText(mancpn.toString(4))

            Timber.tag("EPIC").i("Created .egstore metadata for ${game.appName}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("EPIC").e(e, "Failed to create .egstore metadata")
            Result.failure(e)
        }
    }

    /**
     * Clean up temporary ownership token files after game exits
     */
    fun cleanupOwnershipTokens(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "epic_tokens")
            if (tempDir.exists() && tempDir.isDirectory) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.extension == "ovt") {
                        file.delete()
                        Timber.tag("EPIC").d("Deleted ownership token file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup ownership token files")
        }
    }
}
