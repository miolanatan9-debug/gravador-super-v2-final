package br.com.guga.gravadorsuper.helpers

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer

object AudioHelper {
    fun trimAudio(context: Context, inputPath: String, outputPath: String, startMs: Long, endMs: Long): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(inputPath)
            val trackCount = extractor.trackCount
            if (trackCount == 0) return false

            extractor.selectTrack(0)
            val format = extractor.getTrackFormat(0)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return false

            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackIndex = muxer.addTrack(format)
            muxer.start()

            val maxBufferSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                1024 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (bufferInfo.presentationTimeUs > endMs * 1000) {
                    break
                }

                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {}
            extractor.release()
        }
    }
}
