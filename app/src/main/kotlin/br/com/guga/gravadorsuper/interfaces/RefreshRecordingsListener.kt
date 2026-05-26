package br.com.guga.gravadorsuper.interfaces

import br.com.guga.gravadorsuper.models.Recording

interface RefreshRecordingsListener {
    fun refreshRecordings()

    fun playRecording(recording: Recording, playOnPrepared: Boolean)
}
