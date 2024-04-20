package com.example.downloadmanager

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.downloadmanager.ui.theme.DownloadManagerTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val DOWNLOAD_COMPLETE_ACTION = "android.intent.action.DOWNLOAD_COMPLETE"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DownloadManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    val snackbarHostState = remember { SnackbarHostState() }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState) {
                                Snackbar(snackbarData = it, actionColor = Color(0xFF8AF535))
                            }
                        },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "Download pdf",
                                        fontWeight = FontWeight.W600,
                                    )
                                },
                            )
                        }
                    ) { internalPadding ->
                        SystemBroadcastReceiver(
                            systemAction = DOWNLOAD_COMPLETE_ACTION,
                            onSystemEvent = { intent ->
                                if (intent?.action == DOWNLOAD_COMPLETE_ACTION) {
                                    val id = intent.getLongExtra(
                                        DownloadManager.EXTRA_DOWNLOAD_ID,
                                        -1L
                                    )
                                    if (id != -1L) {
                                        scope.launch {
                                            val result = snackbarHostState
                                                .showSnackbar(
                                                    message = "View pdf",
                                                    actionLabel = "Yes",
                                                    duration = SnackbarDuration.Short
                                                )
                                            when (result) {
                                                SnackbarResult.ActionPerformed -> {
                                                    navigateToDownloadedInvoice()
                                                }

                                                else -> {}
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        Greeting(
                            "Get Pdf", modifier = Modifier
                                .padding(internalPadding)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    private fun navigateToDownloadedInvoice() {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ),
                "Test.pdf"
            )
            val uri = FileProvider.getUriForFile(
                this,
                applicationContext?.packageName + ".provider",
                file
            )
            val intent =
                Intent(Intent.ACTION_VIEW)
            with(intent) {
                setDataAndType(
                    uri,
                    "application/pdf"
                )
                flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
//either user removed or moved the file or file not downloaded correctly.
            Toast.makeText(this, "unexpected error", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            Text(
                text = "Hello $name!",
                modifier = Modifier
                    .clickable { enqueueDownloadRequest("https://www.clickdimensions.com/links/TestPDFfile.pdf") }
                    .align(
                        Alignment.Center
                    ),
                textAlign = TextAlign.Center
            )
        }
    }

    private fun enqueueDownloadRequest(
        url: String
    ) {
        if (url.isEmpty()) {
            Toast.makeText(this, "invalid url", Toast.LENGTH_SHORT).show()
            return
        }
        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(url))
        with(request) {
            setTitle("Test pdf")
            setMimeType("pdf")
            setDescription("Downloading pdf...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "test.pdf"
            )
        }
        val manager: DownloadManager =
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }

    @Composable
    fun SystemBroadcastReceiver(
        systemAction: String,
        onSystemEvent: (intent: Intent?) -> Unit
    ) {
        val context = LocalContext.current
        val currentOnSystemEvent by rememberUpdatedState(onSystemEvent)

        DisposableEffect(context, systemAction) {
            val intentFilter = IntentFilter(systemAction)
            val broadcast = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    currentOnSystemEvent(intent)
                }
            }

            ContextCompat.registerReceiver(
                context, broadcast, intentFilter,
                ContextCompat.RECEIVER_EXPORTED
            )

            onDispose {
                context.unregisterReceiver(broadcast)
            }
        }
    }

}
