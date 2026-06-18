package si.maev.twa

import android.content.Context
import android.os.Bundle
import android.provider.Browser
import androidx.browser.trusted.TokenStore
import androidx.browser.trusted.TrustedWebActivityIntent

class TwaLauncher(
    context: Context,
    providerPackage: String?,
    sessionId: Int?,
    tokenStore: TokenStore
) : com.google.androidbrowserhelper.trusted.TwaLauncher(
    context, providerPackage, sessionId, tokenStore
) {
    override fun onPrepareIntent(intent: TrustedWebActivityIntent): TrustedWebActivityIntent {
        val headers = Bundle()
        headers.putString("vibetype_platform", "android")
        intent.intent.putExtra(Browser.EXTRA_HEADERS, headers)
        return intent
    }
}