package com.hktech.personalexpensetracker.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleDriveService(private val context: Context) {

    private val gson = Gson()

    companion object {
        private const val TAG = "GoogleDriveService"
        private const val BACKUP_FOLDER_NAME = "ExpenseTrackerBackup"
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val BACKUP_FILE_NAME = "expense_tracker_backup.json"
        private const val MIME_TYPE_JSON = "application/json"
        private const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
    }

    fun initializeSignIn(onSignInNeeded: (Intent) -> Unit) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()

        val signInClient = GoogleSignIn.getClient(context, gso)
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            onSignInNeeded(signInClient.signInIntent)
        }
    }

    suspend fun handleSignInResult(data: Intent?): Boolean = withContext(Dispatchers.IO) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Tasks.await(task)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            false
        }
    }

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        try {
            val signInClient = GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN)
            Tasks.await(signInClient.signOut())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
            false
        }
    }

    fun getAccountEmail(): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }

    suspend fun uploadBackup(backupFile: java.io.File, onProgress: (String) -> Unit = {}): DriveUploadResult = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext DriveUploadResult(success = false, error = "Not signed in")

        try {
            onProgress("Getting access token...")
            val accessToken = getAccessTokenFromAccount(account)
            if (accessToken == null) {
                return@withContext DriveUploadResult(
                    success = false,
                    error = "Could not get access token. Please sign out and sign in again."
                )
            }

            onProgress("Finding backup folder...")
            val folderId = findOrCreateBackupFolder(accessToken)
            if (folderId == null) {
                return@withContext DriveUploadResult(success = false, error = "Could not create backup folder")
            }

            onProgress("Deleting old backup...")
            deleteExistingBackup(accessToken, folderId)

            onProgress("Uploading backup...")
            val fileId = uploadFile(accessToken, folderId, backupFile)
            if (fileId == null) {
                return@withContext DriveUploadResult(success = false, error = "Upload failed")
            }

            onProgress("Upload complete!")
            Log.d(TAG, "Uploaded backup: $fileId")

            DriveUploadResult(
                success = true,
                fileId = fileId,
                fileName = BACKUP_FILE_NAME,
                fileSize = backupFile.length(),
                modifiedTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            DriveUploadResult(success = false, error = e.message ?: "Upload failed")
        }
    }

    suspend fun downloadBackup(onProgress: (String) -> Unit = {}): DriveDownloadResult = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext DriveDownloadResult(success = false, error = "Not signed in")

        try {
            onProgress("Finding backup file...")
            val accessToken = getAccessTokenFromAccount(account)
            if (accessToken == null) {
                return@withContext DriveDownloadResult(success = false, error = "Could not get access token")
            }

            val folderId = findBackupFolder(accessToken)
            if (folderId == null) {
                return@withContext DriveDownloadResult(success = false, error = "No backup folder found")
            }

            val fileId = findBackupFile(accessToken, folderId)
            if (fileId == null) {
                return@withContext DriveDownloadResult(success = false, error = "No backup found on Google Drive")
            }

            onProgress("Downloading backup...")
            val json = downloadFile(accessToken, fileId)
            if (json == null) {
                return@withContext DriveDownloadResult(success = false, error = "Download failed")
            }

            onProgress("Download complete!")
            DriveDownloadResult(success = true, jsonContent = json, fileId = fileId)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            DriveDownloadResult(success = false, error = e.message ?: "Download failed")
        }
    }

    suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext false
            val accessToken = getAccessTokenFromAccount(account) ?: return@withContext false
            val folderId = findBackupFolder(accessToken) ?: return@withContext false
            findBackupFile(accessToken, folderId) != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getBackupInfo(): DriveUploadResult? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
            val accessToken = getAccessTokenFromAccount(account) ?: return@withContext null
            val folderId = findBackupFolder(accessToken) ?: return@withContext null
            val fileId = findBackupFile(accessToken, folderId) ?: return@withContext null
            val fileInfo = getFileInfo(accessToken, fileId)

            DriveUploadResult(
                success = true,
                fileId = fileId,
                fileName = BACKUP_FILE_NAME,
                fileSize = fileInfo?.get("size")?.toString()?.toLongOrNull() ?: 0,
                modifiedTime = fileInfo?.get("modifiedTime")?.toString()?.toLongOrNull()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get backup info", e)
            null
        }
    }

    private suspend fun getAccessTokenFromAccount(account: GoogleSignInAccount): String? {
        return try {
            // Try the newer API first (GoogleSignInAccountExtension)
            val extensionMethod = account.javaClass.getMethod("getAccessToken")
            val result = extensionMethod.invoke(account)
            if (result is String) return result

            // Fallback to Task-based approach
            val taskMethod = account.javaClass.getMethod("getIdToken")
            @Suppress("UNCHECKED_CAST")
            val task = taskMethod.invoke(account) as? com.google.android.gms.tasks.Task<String>
            if (task != null) Tasks.await(task) else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get access token via reflection: ${e.message}", e)
            null
        }
    }

    private fun findBackupFolder(accessToken: String): String? {
        val query = URLEncoder.encode("name='$BACKUP_FOLDER_NAME' and mimeType='$MIME_TYPE_FOLDER' and trashed=false", "UTF-8")
        val url = URL("$DRIVE_API_BASE/files?q=$query&spaces=drive&fields=files(id,name)")

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.requestMethod = "GET"

        return try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonObj = gson.fromJson(response, Map::class.java)
                @Suppress("UNCHECKED_CAST")
                val files = jsonObj["files"] as? List<Map<String, Any>>
                files?.firstOrNull()?.get("id") as? String
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun findOrCreateBackupFolder(accessToken: String): String? {
        findBackupFolder(accessToken)?.let { return it }

        val metadata = gson.toJson(mapOf("name" to BACKUP_FOLDER_NAME, "mimeType" to MIME_TYPE_FOLDER))
        val url = URL("$DRIVE_API_BASE/files?fields=id")

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", MIME_TYPE_JSON)
        conn.requestMethod = "POST"
        conn.doOutput = true

        return try {
            conn.outputStream.use { it.write(metadata.toByteArray()) }

            if (conn.responseCode == HttpURLConnection.HTTP_CREATED) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                val jsonObj = gson.fromJson(response, Map::class.java)
                jsonObj["id"] as? String
            } else {
                Log.e(TAG, "Failed to create folder: ${conn.responseCode}")
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun findBackupFile(accessToken: String, folderId: String): String? {
        val query = URLEncoder.encode("name='$BACKUP_FILE_NAME' and '$folderId' in parents and trashed=false", "UTF-8")
        val url = URL("$DRIVE_API_BASE/files?q=$query&spaces=drive&fields=files(id)")

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.requestMethod = "GET"

        return try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonObj = gson.fromJson(response, Map::class.java)
                @Suppress("UNCHECKED_CAST")
                val files = jsonObj["files"] as? List<Map<String, Any>>
                files?.firstOrNull()?.get("id") as? String
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun deleteExistingBackup(accessToken: String, folderId: String) {
        val fileId = findBackupFile(accessToken, folderId) ?: return
        val url = URL("$DRIVE_API_BASE/files/$fileId")

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.requestMethod = "DELETE"

        try {
            if (conn.responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                Log.d(TAG, "Deleted old backup: $fileId")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun uploadFile(accessToken: String, folderId: String, file: java.io.File): String? {
        val boundary = "boundary_${System.currentTimeMillis()}"
        val url = URL("$DRIVE_UPLOAD_URL?uploadType=multipart")

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        conn.requestMethod = "POST"
        conn.doOutput = true

        return try {
            val content = buildMultipartContent(boundary, folderId, file)
            conn.outputStream.use { it.write(content) }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                val jsonObj = gson.fromJson(response, Map::class.java)
                jsonObj["id"] as? String
            } else {
                Log.e(TAG, "Upload failed: ${conn.responseCode}")
                val errorReader = BufferedReader(InputStreamReader(conn.errorStream))
                val error = errorReader.readText()
                errorReader.close()
                Log.e(TAG, "Error: $error")
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun buildMultipartContent(boundary: String, folderId: String, file: java.io.File): ByteArray {
        val body = StringBuilder()
        body.append("--$boundary\r\n")
        body.append("Content-Type: $MIME_TYPE_JSON\r\n\r\n")
        body.append(gson.toJson(mapOf("name" to BACKUP_FILE_NAME, "parents" to listOf(folderId))))
        body.append("\r\n")
        body.append("--$boundary\r\n")
        body.append("Content-Type: $MIME_TYPE_JSON\r\n\r\n")
        body.append(file.readText())
        body.append("\r\n")
        body.append("--$boundary--\r\n")
        return body.toString().toByteArray()
    }

    private fun downloadFile(accessToken: String, fileId: String): String? {
        val url = URL("$DRIVE_API_BASE/files/$fileId?alt=media")

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.requestMethod = "GET"

        return try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val content = reader.readText()
                reader.close()
                content
            } else {
                Log.e(TAG, "Download failed: ${conn.responseCode}")
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun getFileInfo(accessToken: String, fileId: String): Map<String, Any>? {
        val url = URL("$DRIVE_API_BASE/files/$fileId?fields=id,name,size,modifiedTime")

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.requestMethod = "GET"

        return try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(response, Map::class.java) as? Map<String, Any>
            } else null
        } finally {
            conn.disconnect()
        }
    }
}

data class DriveUploadResult(
    val success: Boolean,
    val error: String? = null,
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0,
    val modifiedTime: Long? = null
)

data class DriveDownloadResult(
    val success: Boolean,
    val error: String? = null,
    val jsonContent: String? = null,
    val fileId: String? = null
)