package si.maev.twa

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

private const val STATE_IS_SETTINGS_OPENED = "isSettingsOpened"

// Since Android 14 the location delegation cannot show the permission dialog itself, see `DelegationService`, so the web app opens this activity from a tap, which the browser may start.
class LocationPermissionActivity : Activity() {
    private var isRationaleShownBefore = false
    private var isSettingsOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            isSettingsOpened = savedInstanceState.getBoolean(STATE_IS_SETTINGS_OPENED)
            return
        }
        if (isGranted()) return finish()

        isRationaleShownBefore =
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 0
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        // Without a rationale before and after, the system showed no dialog because the user denied it for good, so the settings are the place left to allow it.
        val isDenied = isRationaleShownBefore ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        if (isGranted() || isDenied) return finish()

        isSettingsOpened = true
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
        )
    }

    // The settings cover this activity completely, so a restart means the user came back from them.
    override fun onRestart() {
        super.onRestart()
        if (isSettingsOpened) finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_IS_SETTINGS_OPENED, isSettingsOpened)
    }

    private fun isGranted() =
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
