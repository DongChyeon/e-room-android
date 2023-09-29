package com.dongchyeon.e_room

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dongchyeon.e_room.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        CookieManager.getInstance()

        webView = binding.webview
        if (isNetworkConnected()) {

            with(webView) {
                loadUrl(BuildConfig.BASE_URL)

                webViewClient = object : WebViewClient() {
                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        Log.e("e-room", "Http Error : $errorResponse")
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        Log.e("e-room", "SSL Error : $error")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e("e-room", "Error : $error")
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        super.onGeolocationPermissionsShowPrompt(origin, callback)
                        callback?.invoke(origin, true, false)
                        requestLocationPermission()
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.apply {
                            Log.d(
                                "e-room",
                                "${message()} -- From line ${lineNumber()} of ${sourceId()}"
                            )
                        }
                        return true
                    }
                }

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Chrome/56.0.0.0 Mobile"

                settings.setGeolocationEnabled(true)
            }

            onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        } else {
            showSnackBar(binding.root, "인터넷이 연결되어 있지 않아 앱이 종료됩니다.")
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 1000)
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return coarseLocationPermission == PackageManager.PERMISSION_GRANTED && fineLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (!isLocationPermissionGranted()) {
                Toast.makeText(
                    applicationContext,
                    "위치 권한을 허용해야 현재 위치를 알 수 있습니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showSnackBar(rootView: View, message: String) {
        val snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
        snackBar.show()
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}