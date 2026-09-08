package com.brodari.captagram

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.min

class VideoAudioExtractor(
    private val context: Context
) {

    companion object {
        private const val TARGET_SAMPLE_RATE = 16_000
    }

    fun extract(uri: Uri): FloatArray {
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(
                context,
                uri,
                emptyMap()
            )

            val audioTrackIndex = findAudioTrack(extractor)

            if (audioTrackIndex < 0) {
                throw IllegalStateException("Video does not contain an audio track")
            }

            extractor.selectTrack(audioTrackIndex)

            val inputFormat = extractor.getTrackFormat(audioTrackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalStateException("Audio MIME type is missing")

            val decoder = MediaCodec.createDecoderByType(mime)

            try {
                decoder.configure(
                    inputFormat,
                    null,
                    null,
                    0
                )

                decoder.start()

                return decodeAudio(
                    extractor = extractor,
                    decoder = decoder,
                    inputFormat = inputFormat
                )

            } finally {
                try {
                    decoder.stop()
                } catch (_: IllegalStateException) {
                }

                decoder.release()
            }

        } finally {
            extractor.release()
        }
    }

    private fun findAudioTrack(
        extractor: MediaExtractor
    ): Int {

        for (index in 0 until extractor.trackCount) {

            val format = extractor.getTrackFormat(index)

            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: continue

            if (mime.startsWith("audio/")) {
                return index
            }
        }

        return -1
    }

    private fun decodeAudio(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        inputFormat: MediaFormat
    ): FloatArray {

        val outputSamples = ArrayList<Float>()

        val bufferInfo = MediaCodec.BufferInfo()

        var inputFinished = false
        var outputFinished = false

        var sampleRate =
            inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

        var channelCount =
            inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        var pcmEncoding =
            if (inputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }

        while (!outputFinished) {

            if (!inputFinished) {

                val inputBufferIndex =
                    decoder.dequeueInputBuffer(10_000)

                if (inputBufferIndex >= 0) {

                    val inputBuffer =
                        decoder.getInputBuffer(inputBufferIndex)
                            ?: throw IllegalStateException(
                                "Unable to access decoder input buffer"
                            )

                    inputBuffer.clear()

                    val sampleSize =
                        extractor.readSampleData(
                            inputBuffer,
                            0
                        )

                    if (sampleSize < 0) {

                        decoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )

                        inputFinished = true

                    } else {

                        val presentationTimeUs =
                            extractor.sampleTime

                        decoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            sampleSize,
                            presentationTimeUs,
                            0
                        )

                        extractor.advance()
                    }
                }
            }

            when (
                val outputBufferIndex =
                    decoder.dequeueOutputBuffer(
                        bufferInfo,
                        10_000
                    )
            ) {

                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (inputFinished) {
                        continue
                    }
                }

                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {

                    val outputFormat =
                        decoder.outputFormat

                    if (outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate =
                            outputFormat.getInteger(
                                MediaFormat.KEY_SAMPLE_RATE
                            )
                    }

                    if (outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount =
                            outputFormat.getInteger(
                                MediaFormat.KEY_CHANNEL_COUNT
                            )
                    }

                    if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        pcmEncoding =
                            outputFormat.getInteger(
                                MediaFormat.KEY_PCM_ENCODING
                            )
                    }
                }

                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    // Deprecated on newer Android versions.
                }

                else -> {

                    if (outputBufferIndex >= 0) {

                        val outputBuffer =
                            decoder.getOutputBuffer(
                                outputBufferIndex
                            )

                        if (outputBuffer != null && bufferInfo.size > 0) {

                            outputBuffer.position(
                                bufferInfo.offset
                            )

                            outputBuffer.limit(
                                bufferInfo.offset + bufferInfo.size
                            )

                            appendPcmSamples(
                                buffer = outputBuffer,
                                channelCount = channelCount,
                                pcmEncoding = pcmEncoding,
                                outputSamples = outputSamples
                            )
                        }

                        val endOfStream =
                            (bufferInfo.flags and
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        decoder.releaseOutputBuffer(
                            outputBufferIndex,
                            false
                        )

                        if (endOfStream) {
                            outputFinished = true
                        }
                    }
                }
            }
        }

        if (outputSamples.isEmpty()) {
            throw IllegalStateException("Unable to decode audio from video")
        }

        return resampleTo16k(
            input = outputSamples,
            sourceSampleRate = sampleRate
        )
    }

    private fun appendPcmSamples(
        buffer: ByteBuffer,
        channelCount: Int,
        pcmEncoding: Int,
        outputSamples: MutableList<Float>
    ) {

        val littleEndian =
            buffer.order(ByteOrder.LITTLE_ENDIAN)

        when (pcmEncoding) {

            AudioFormat.ENCODING_PCM_FLOAT -> {

                val bytesPerSample = 4
                val frameSize =
                    bytesPerSample * channelCount

                while (littleEndian.remaining() >= frameSize) {

                    var mono = 0f

                    repeat(channelCount) {
                        mono += littleEndian.float
                    }

                    outputSamples.add(
                        mono / channelCount
                    )
                }
            }

            AudioFormat.ENCODING_PCM_8BIT -> {

                val frameSize = channelCount

                while (littleEndian.remaining() >= frameSize) {

                    var mono = 0f

                    repeat(channelCount) {
                        val unsignedSample =
                            littleEndian.get().toInt() and 0xFF

                        mono +=
                            (unsignedSample - 128) / 128f
                    }

                    outputSamples.add(
                        mono / channelCount
                    )
                }
            }

            else -> {

                val bytesPerSample = 2
                val frameSize =
                    bytesPerSample * channelCount

                while (littleEndian.remaining() >= frameSize) {

                    var mono = 0f

                    repeat(channelCount) {

                        val sample =
                            littleEndian.short.toInt()

                        mono +=
                            sample / 32768f
                    }

                    outputSamples.add(
                        mono / channelCount
                    )
                }
            }
        }
    }

    private fun resampleTo16k(
        input: List<Float>,
        sourceSampleRate: Int
    ): FloatArray {

        if (sourceSampleRate == TARGET_SAMPLE_RATE) {
            return input.toFloatArray()
        }

        if (sourceSampleRate <= 0) {
            throw IllegalStateException(
                "Invalid source audio sample rate"
            )
        }

        val outputSize =
            (input.size.toDouble() *
                    TARGET_SAMPLE_RATE /
                    sourceSampleRate)
                .toInt()

        if (outputSize <= 0) {
            throw IllegalStateException(
                "Audio is too short to resample"
            )
        }

        val output =
            FloatArray(outputSize)

        val ratio =
            sourceSampleRate.toDouble() /
                    TARGET_SAMPLE_RATE

        for (index in output.indices) {

            val sourcePosition =
                index * ratio

            val sourceIndex =
                floor(sourcePosition).toInt()

            val nextIndex =
                min(
                    sourceIndex + 1,
                    input.lastIndex
                )

            val fraction =
                (sourcePosition - sourceIndex).toFloat()

            val first =
                input[
                    sourceIndex.coerceIn(
                        0,
                        input.lastIndex
                    )
                ]

            val second =
                input[nextIndex]

            output[index] =
                first + (second - first) * fraction
        }

        return output
    }
}
