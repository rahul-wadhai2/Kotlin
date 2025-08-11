package com.jejecomms.realtimechatfeature.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.ContextCompat
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.utils.Constants.PACKAGE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Object containing utility functions related to permissions.
 */
object PermissionUtils {

    /**
     * Checks and requests the POST_NOTIFICATIONS permission for API 33 and above.
     * It handles the rationale flow by showing a snackbar if needed.
     *
     * @param context The context from the composable.
     * @param activity The ComponentActivity to check for rationale.
     * @param permissionLauncher The launcher to request the permission.
     * @param snackbarHostState The state for showing a snackbar with rationale.
     * @param coroutineScope The coroutine scope to launch snackbar actions.
     */
    fun checkAndRequestNotificationPermission(
        context: Context,
        activity: ComponentActivity,
        permissionLauncher: ActivityResultLauncher<String>,
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isPermissionGranted) {
                // The 'shouldShowRequestPermissionRationale' must be called from an activity context
                if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    coroutineScope.launch {
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.notification_permission_message),
                            actionLabel = context.getString(R.string.action_settings_label)
                        )
                        if (snackbarResult == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts(PACKAGE, context.packageName, null)
                                }
                            activity.startActivity(intent)
                        }
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

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
        onPermissionGranted: () -> Unit,
        permission: String
    ) {
        val isPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (isPermissionGranted) {
            onPermissionGranted()
        } else {
            // Request the appropriate permission
            permissionLauncher.launch(permission)
        }
    }
}