package com.example.icecastcontroller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StreamSourceRepository(application)

    private val _sources = MutableLiveData<List<StreamSource>>()
    val sources: LiveData<List<StreamSource>> = _sources

    private val _selectedSource = MutableLiveData<StreamSource?>()
    val selectedSource: LiveData<StreamSource?> = _selectedSource

    private val _castState = MutableLiveData(CastState.DISCONNECTED)
    val castState: LiveData<CastState> = _castState

    private val _volume = MutableLiveData(0.5f)
    val volume: LiveData<Float> = _volume

    enum class CastState {
        DISCONNECTED, CONNECTING, PLAYING, PAUSED, BUFFERING
    }

    init {
        loadSources()
    }

    fun loadSources() {
        _sources.value = repository.getSources()
    }

    fun selectSource(source: StreamSource) {
        _selectedSource.value = source
    }

    fun addSource(source: StreamSource) {
        repository.addSource(source)
        loadSources()
    }

    fun removeSource(id: String) {
        repository.removeSource(id)
        loadSources()
        if (_selectedSource.value?.id == id) {
            _selectedSource.value = null
        }
    }

    fun updateCastState(state: CastState) {
        _castState.value = state
    }

    fun updateVolume(v: Float) {
        _volume.value = v
    }
}
