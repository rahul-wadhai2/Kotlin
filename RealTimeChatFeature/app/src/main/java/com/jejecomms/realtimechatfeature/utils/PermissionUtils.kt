package com.jejecomms.realtimechatfeature.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Object containing utility functions related to permissions.
 */
object PermissionUtils {
    /**
     * Checks if the legacy READ_EXTERNAL_STORAGE permission is granted for older devices.
     * This function is only used for devices with API 32 and below.
     *
     * @param context The context from the composable.
     * @param permissionLauncher The launcher to request the permission.
     * @param onPermissionGranted A lambda to execute if permission is already granted.
     */
    fun checkAndRequestLegacyPermission(
        context: Context,
        permissionLauncher: ActivityResultLauncher<String>,
        onPermissionGranted: () -> Unit
    ) {
        val isPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (isPermissionGranted) {
            onPermissionGranted()
        } else {
            // Request the appropriate permission
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}