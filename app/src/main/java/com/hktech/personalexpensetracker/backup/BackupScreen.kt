package com.hktech.personalexpensetracker.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    backupManager: BackupManager,
    googleDriveService: GoogleDriveService,
    onBackupComplete: () -> Unit,
    onRestoreComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Sign-in state
    var isSignedIn by remember { mutableStateOf(googleDriveService.isSignedIn()) }
    var accountEmail by remember { mutableStateOf(googleDriveService.getAccountEmail()) }

    // Backup info
    var backupInfo by remember { mutableStateOf<DriveUploadResult?>(null) }
    var lastLocalBackup by remember { mutableStateOf<Date?>(null) }
    var pendingRestoreData by remember { mutableStateOf<BackupData?>(null) }

    // Dialog states
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreProgressDialog by remember { mutableStateOf(false) }
    var restoreProgress by remember { mutableStateOf("") }
    var restoreResult by remember { mutableStateOf<RestoreResult?>(null) }

    // Activity result launchers
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            val success = googleDriveService.handleSignInResult(result.data)
            isSignedIn = success
            accountEmail = googleDriveService.getAccountEmail()
            if (success) {
                statusMessage = "Signed in as ${accountEmail}"
                isError = false
            } else {
                statusMessage = "Sign in failed"
                isError = true
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportedUri ->
            scope.launch {
                isLoading = true
                statusMessage = "Exporting backup..."
                val success = backupManager.exportToUri(exportedUri)
                statusMessage = if (success) "Backup exported successfully" else "Export failed"
                isError = !success
                isLoading = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importedUri ->
            scope.launch {
                isLoading = true
                val backupData = backupManager.importFromUri(importedUri)
                if (backupData != null) {
                    pendingRestoreData = backupData
                    statusMessage = "Backup loaded: ${backupData.transactions.size} transactions"
                    isError = false
                    showRestoreConfirmDialog = true
                } else {
                    statusMessage = "Failed to read backup file"
                    isError = true
                }
                isLoading = false
            }
        }
    }

    // Check for existing Drive backup info
    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            backupInfo = googleDriveService.getBackupInfo()
        }
        backupManager.getLatestBackupFile()?.let {
            lastLocalBackup = Date(it.lastModified())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Backup & Restore",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Keep your data safe by backing up to Google Drive or local storage",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Google Drive Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Google Drive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (isSignedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Signed in as",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = accountEmail ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(onClick = {
                            scope.launch {
                                googleDriveService.signOut()
                                isSignedIn = false
                                accountEmail = null
                                backupInfo = null
                            }
                        }) {
                            Text("Sign Out")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    backupInfo?.let { info ->
                        if (info.success) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Last backup", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = formatBackupDate(info.modifiedTime),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Size", style = MaterialTheme.typography.labelMedium)
                                Text(text = formatFileSize(info.fileSize), style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                statusMessage = "Creating backup..."
                                isError = false

                                try {
                                    val db = com.hktech.personalexpensetracker.data.AppDatabase.get(context)
                                    val backupFile = backupManager.createFullBackup(db, "1.0")
                                    if (backupFile != null) {
                                        val result = googleDriveService.uploadBackup(backupFile) { progress ->
                                            statusMessage = progress
                                        }
                                        if (result.success) {
                                            statusMessage = "Backup uploaded to Google Drive!"
                                            backupInfo = result
                                            onBackupComplete()
                                        } else {
                                            statusMessage = result.error ?: "Upload failed"
                                            isError = true
                                        }
                                    } else {
                                        statusMessage = "Failed to create backup file"
                                        isError = true
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Error: ${e.message}"
                                    isError = true
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (backupInfo == null) "Upload Backup" else "Update Backup")
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                statusMessage = "Checking for backup..."
                                isError = false

                                val result = googleDriveService.downloadBackup { progress ->
                                    statusMessage = progress
                                }

                                if (result.success && result.jsonContent != null) {
                                    showRestoreProgressDialog = true
                                    restoreProgress = "Preparing restore..."
                                    restoreResult = null

                                    val db = com.hktech.personalexpensetracker.data.AppDatabase.get(context)
                                    restoreResult = backupManager.restoreFromJson(result.jsonContent!!, db) { progress ->
                                        restoreProgress = progress
                                    }

                                    if (restoreResult?.success == true) {
                                        restoreProgress = "Restore complete!"
                                        onRestoreComplete()
                                    } else {
                                        restoreProgress = "Restore failed: ${restoreResult?.error}"
                                    }
                                } else {
                                    statusMessage = result.error ?: "No backup found on Google Drive"
                                    isError = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && backupInfo != null
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restore from Drive")
                    }

                    if (backupInfo == null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No backup found on Google Drive. Create a backup to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(text = "Sign in to sync your backup to Google Drive", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            googleDriveService.initializeSignIn { intent ->
                                signInLauncher.launch(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign in with Google")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Local Backup Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Local Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                lastLocalBackup?.let {
                    Text(
                        text = "Last local backup: ${formatBackupDate(it.time)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    text = "Saved to: ${backupManager.getBackupPath()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                statusMessage = "Creating backup..."
                                isError = false

                                try {
                                    val db = com.hktech.personalexpensetracker.data.AppDatabase.get(context)
                                    val backupFile = backupManager.createFullBackup(db, "1.0")
                                    if (backupFile != null) {
                                        lastLocalBackup = Date()
                                        statusMessage = "Backup saved to Downloads/ExpenseTrackerBackups/"
                                        isError = false
                                        onBackupComplete()
                                    } else {
                                        statusMessage = "Failed to save backup"
                                        isError = true
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Error: ${e.message}"
                                    isError = true
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Backup")
                    }

                    OutlinedButton(
                        onClick = {
                            exportLauncher.launch("expense_tracker_backup_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.json")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export")
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import Backup File")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Status Message
        statusMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isError) Icons.Default.Error else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { statusMessage = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "About Backups",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• Backups include all transactions, categories, merchants, accounts, and payment channels\n" +
                            "• Google Drive backups are stored securely in your Google account\n" +
                            "• Restoring a backup will replace all current data\n" +
                            "• Local backups are saved to Downloads/ExpenseTrackerBackups/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Restore confirmation dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore Backup?") },
            text = {
                Column {
                    Text("This will replace all current data with the backup data. This action cannot be undone.")
                    Spacer(Modifier.height(12.dp))
                    pendingRestoreData?.let { data ->
                        Text(
                            text = "Backup contains:\n" +
                                    "• ${data.transactions.size} transactions\n" +
                                    "• ${data.categories.size} categories\n" +
                                    "• ${data.merchants.size} merchants\n" +
                                    "• ${data.accounts.size} accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreData?.let { data ->
                            scope.launch {
                                showRestoreProgressDialog = true
                                restoreProgress = "Preparing restore..."
                                restoreResult = null

                                val db = com.hktech.personalexpensetracker.data.AppDatabase.get(context)
                                val json = com.google.gson.Gson().toJson(data)
                                restoreResult = backupManager.restoreFromJson(json, db) { progress ->
                                    restoreProgress = progress
                                }

                                if (restoreResult?.success == true) {
                                    restoreProgress = "Restore complete!"
                                    onRestoreComplete()
                                } else {
                                    restoreProgress = "Restore failed: ${restoreResult?.error}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreData = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore progress dialog
    if (showRestoreProgressDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Restoring...") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (restoreResult == null) {
                        CircularProgressIndicator()
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(text = restoreProgress, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                if (restoreResult != null) {
                    Button(onClick = { showRestoreProgressDialog = false }) {
                        Text("Done")
                    }
                }
            }
        )
    }
}

private fun formatBackupDate(timestamp: Long?): String {
    if (timestamp == null) return "Never"
    val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}