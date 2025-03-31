package com.trendflick.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

/**
 * Utility class for handling permissions in the app
 */
object PermissionUtils {

    /**
     * Check if a permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if multiple permissions are granted
     */
    fun arePermissionsGranted(context: Context, permissions: List<String>): Boolean {
        return permissions.all { permission ->
            isPermissionGranted(context, permission)
        }
    }

    /**
     * Composable to request a permission
     */
    @Composable
    fun RequestPermission(
        permission: String,
        onPermissionResult: (Boolean) -> Unit
    ) {
        val permissionState = remember { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionState.value = isGranted
            onPermissionResult(isGranted)
        }

        LaunchedEffect(Unit) {
            launcher.launch(permission)
        }
    }

    /**
     * Composable to request multiple permissions
     */
    @Composable
    fun RequestMultiplePermissions(
        permissions: List<String>,
        onPermissionsResult: (Map<String, Boolean>) -> Unit
    ) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            onPermissionsResult(permissionsMap)
        }

        LaunchedEffect(Unit) {
            launcher.launch(permissions.toTypedArray())
        }
    }
}
