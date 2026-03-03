package com.example.icecastcontroller

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.icecastcontroller.databinding.ActivityMainBinding
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.cast.MediaStatus

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var castContext: CastContext
    private var sessionManager: SessionManager? = null
    private var remoteMediaClient: RemoteMediaClient? = null

    private lateinit var sourceAdapter: SourceAdapter

    // Callback für Cast-Session-Änderungen
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            viewModel.updateCastState(MainViewModel.CastState.CONNECTING)
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            invalidateOptionsMenu()
            remoteMediaClient = session.remoteMediaClient
            remoteMediaClient?.registerCallback(mediaClientCallback)
            viewModel.updateCastState(MainViewModel.CastState.DISCONNECTED)
            // Volume aus dem Cast-Gerät lesen
            val vol = session.volume.toFloat()
            viewModel.updateVolume(vol)
            binding.seekbarVolume.progress = (vol * 100).toInt()
            showToast("Verbunden mit Cast-Gerät")
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            viewModel.updateCastState(MainViewModel.CastState.DISCONNECTED)
            showToast("Verbindung fehlgeschlagen (Code $error)")
        }

        override fun onSessionEnding(session: CastSession) {}

        override fun onSessionEnded(session: CastSession, error: Int) {
            remoteMediaClient?.unregisterCallback(mediaClientCallback)
            remoteMediaClient = null
            invalidateOptionsMenu()
            viewModel.updateCastState(MainViewModel.CastState.DISCONNECTED)
            showToast("Cast-Verbindung getrennt")
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {}

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            invalidateOptionsMenu()
            remoteMediaClient = session.remoteMediaClient
            remoteMediaClient?.registerCallback(mediaClientCallback)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {}

        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    // Callback für Media-Status-Änderungen
    private val mediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            val playerState = remoteMediaClient?.mediaStatus?.playerState
            val state = when (playerState) {
                MediaStatus.PLAYER_STATE_PLAYING -> MainViewModel.CastState.PLAYING
                MediaStatus.PLAYER_STATE_PAUSED -> MainViewModel.CastState.PAUSED
                MediaStatus.PLAYER_STATE_BUFFERING -> MainViewModel.CastState.BUFFERING
                else -> MainViewModel.CastState.DISCONNECTED
            }
            viewModel.updateCastState(state)
        }
    }

    private val addSourceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val name = data.getStringExtra(AddSourceActivity.EXTRA_NAME) ?: return@registerForActivityResult
            val url = data.getStringExtra(AddSourceActivity.EXTRA_URL) ?: return@registerForActivityResult
            val mime = data.getStringExtra(AddSourceActivity.EXTRA_MIME) ?: "audio/mpeg"
            viewModel.addSource(StreamSource(name = name, url = url, mimeType = mime))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        castContext = CastContext.getSharedInstance(this)
        sessionManager = castContext.sessionManager

        setupRecyclerView()
        setupObservers()
        setupControls()
    }

    private fun setupRecyclerView() {
        sourceAdapter = SourceAdapter(
            onSelect = { source ->
                viewModel.selectSource(source)
                sourceAdapter.setSelected(source.id)
            },
            onDelete = { source ->
                viewModel.removeSource(source.id)
            }
        )
        binding.recyclerSources.adapter = sourceAdapter
    }

    private fun setupObservers() {
        viewModel.sources.observe(this) { sources ->
            sourceAdapter.submitList(sources)
            binding.tvEmptySources.visibility = if (sources.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.selectedSource.observe(this) { source ->
            if (source != null) {
                binding.tvSelectedSource.text = source.name
                binding.tvSelectedUrl.text = source.url
                binding.cardSelectedSource.visibility = View.VISIBLE
            } else {
                binding.cardSelectedSource.visibility = View.GONE
            }
            updatePlayButton()
        }

        viewModel.castState.observe(this) { state ->
            updateCastStateUI(state)
        }
    }

    private fun setupControls() {
        binding.fabAddSource.setOnClickListener {
            addSourceLauncher.launch(Intent(this, AddSourceActivity::class.java))
        }

        binding.btnPlay.setOnClickListener {
            val source = viewModel.selectedSource.value
            if (source == null) {
                showToast("Bitte zuerst eine Quelle auswählen")
                return@setOnClickListener
            }
            val session = castContext.sessionManager.currentCastSession
            if (session == null) {
                showToast("Kein Cast-Gerät verbunden. Bitte zuerst über das Cast-Symbol verbinden.")
                return@setOnClickListener
            }
            castStream(source)
        }

        binding.btnStop.setOnClickListener {
            remoteMediaClient?.stop()
            viewModel.updateCastState(MainViewModel.CastState.DISCONNECTED)
        }

        binding.btnPause.setOnClickListener {
            val client = remoteMediaClient ?: return@setOnClickListener
            if (client.isPlaying) {
                client.pause()
            } else {
                client.play()
            }
        }

        binding.seekbarVolume.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val vol = progress / 100.0
                    val session = castContext.sessionManager.currentCastSession
                    session?.volume = vol
                    viewModel.updateVolume(progress / 100f)
                    binding.tvVolumeValue.text = "$progress%"
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Volume-Buttons
        binding.btnVolDown.setOnClickListener {
            val current = binding.seekbarVolume.progress
            binding.seekbarVolume.progress = maxOf(0, current - 5)
        }
        binding.btnVolUp.setOnClickListener {
            val current = binding.seekbarVolume.progress
            binding.seekbarVolume.progress = minOf(100, current + 5)
        }
    }

    private fun castStream(source: StreamSource) {
        val session = castContext.sessionManager.currentCastSession ?: return

        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, source.name)
            putString(MediaMetadata.KEY_SUBTITLE, source.url)
        }

        val mediaInfo = MediaInfo.Builder(source.url)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(source.mimeType)
            .setMetadata(mediaMetadata)
            .build()

        val client = session.remoteMediaClient ?: run {
            remoteMediaClient = session.remoteMediaClient
            session.remoteMediaClient
        } ?: return

        client.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()
        ).addStatusListener { status ->
            if (!status.isSuccess) {
                showToast("Fehler beim Laden des Streams (${status.statusCode})")
            }
        }

        viewModel.updateCastState(MainViewModel.CastState.BUFFERING)
    }

    private fun updateCastStateUI(state: MainViewModel.CastState) {
        val (statusText, statusColor, playVisible, pauseVisible, stopVisible) = when (state) {
            MainViewModel.CastState.DISCONNECTED -> CastUIState("Bereit", R.color.status_idle, true, false, false)
            MainViewModel.CastState.CONNECTING -> CastUIState("Verbinde…", R.color.status_buffering, false, false, false)
            MainViewModel.CastState.BUFFERING -> CastUIState("Buffert…", R.color.status_buffering, false, true, true)
            MainViewModel.CastState.PLAYING -> CastUIState("▶ Spielt", R.color.status_playing, false, true, true)
            MainViewModel.CastState.PAUSED -> CastUIState("⏸ Pausiert", R.color.status_paused, true, true, true)
        }

        binding.tvCastStatus.text = statusText
        binding.tvCastStatus.setTextColor(getColor(statusColor))
        binding.btnPlay.visibility = if (playVisible) View.VISIBLE else View.GONE
        binding.btnPause.visibility = if (pauseVisible) View.VISIBLE else View.GONE
        binding.btnStop.visibility = if (stopVisible) View.VISIBLE else View.GONE

        // Pause-Button Text anpassen
        if (state == MainViewModel.CastState.PAUSED) {
            binding.btnPause.text = "▶ Weiter"
        } else {
            binding.btnPause.text = "⏸ Pause"
        }
    }

    private fun updatePlayButton() {
        val hasSource = viewModel.selectedSource.value != null
        binding.btnPlay.isEnabled = hasSource
    }

    data class CastUIState(
        val statusText: String,
        val statusColor: Int,
        val playVisible: Boolean,
        val pauseVisible: Boolean,
        val stopVisible: Boolean
    )

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        CastButtonFactory.setUpMediaRouteButton(
            applicationContext,
            menu,
            R.id.media_route_menu_item
        )
        return true
    }

    override fun onResume() {
        super.onResume()
        sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        // Aktuelle Session wiederherstellen falls vorhanden
        val currentSession = castContext.sessionManager.currentCastSession
        if (currentSession != null) {
            remoteMediaClient = currentSession.remoteMediaClient
            remoteMediaClient?.registerCallback(mediaClientCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        sessionManager?.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
