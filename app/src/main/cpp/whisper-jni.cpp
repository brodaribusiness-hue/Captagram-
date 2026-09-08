#include <jni.h>
#include <string>

#include "whisper.h"

static whisper_context* getContext(jlong contextPtr) {
    return reinterpret_cast<whisper_context*>(contextPtr);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_initContext(
        JNIEnv* env,
        jobject /* thiz */,
        jstring modelPath
) {
    if (modelPath == nullptr) {
        return 0;
    }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    if (path == nullptr) {
        return 0;
    }

    whisper_context_params params = whisper_context_default_params();

    whisper_context* context =
            whisper_init_from_file_with_params(path, params);

    env->ReleaseStringUTFChars(modelPath, path);

    return reinterpret_cast<jlong>(context);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_freeContext(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong contextPtr
) {
    whisper_context* context = getContext(contextPtr);

    if (context != nullptr) {
        whisper_free(context);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_fullTranscribe(
        JNIEnv* env,
        jobject /* thiz */,
        jlong contextPtr,
        jint numThreads,
        jfloatArray audioData
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr || audioData == nullptr) {
        return;
    }

    const jsize audioSize = env->GetArrayLength(audioData);

    if (audioSize <= 0) {
        return;
    }

    jfloat* samples =
            env->GetFloatArrayElements(audioData, nullptr);

    if (samples == nullptr) {
        return;
    }

    whisper_full_params params =
            whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;

    params.translate = false;
    params.language = "en";

    params.n_threads = numThreads > 0 ? numThreads : 1;

    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;

    params.token_timestamps = true;

    const int result = whisper_full(
            context,
            params,
            samples,
            static_cast<int>(audioSize)
    );

    env->ReleaseFloatArrayElements(
            audioData,
            samples,
            JNI_ABORT
    );

    (void) result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextSegmentCount(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong contextPtr
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return 0;
    }

    return whisper_full_n_segments(context);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextSegment(
        JNIEnv* env,
        jobject /* thiz */,
        jlong contextPtr,
        jint index
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return env->NewStringUTF("");
    }

    const int count = whisper_full_n_segments(context);

    if (index < 0 || index >= count) {
        return env->NewStringUTF("");
    }

    const char* text =
            whisper_full_get_segment_text(context, index);

    if (text == nullptr) {
        return env->NewStringUTF("");
    }

    return env->NewStringUTF(text);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextSegmentT0(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong contextPtr,
        jint index
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return 0;
    }

    return static_cast<jlong>(
            whisper_full_get_segment_t0(context, index)
    );
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextSegmentT1(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong contextPtr,
        jint index
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return 0;
    }

    return static_cast<jlong>(
            whisper_full_get_segment_t1(context, index)
    );
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextTokenCount(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong contextPtr,
        jint segmentIndex
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return 0;
    }

    const int segmentCount =
            whisper_full_n_segments(context);

    if (segmentIndex < 0 || segmentIndex >= segmentCount) {
        return 0;
    }

    return whisper_full_n_tokens(
            context,
            segmentIndex
    );
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextToken(
        JNIEnv* env,
        jobject /* thiz */,
        jlong contextPtr,
        jint segmentIndex,
        jint tokenIndex
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return env->NewStringUTF("");
    }

    const int segmentCount =
            whisper_full_n_segments(context);

    if (segmentIndex < 0 || segmentIndex >= segmentCount) {
        return env->NewStringUTF("");
    }

    const int tokenCount =
            whisper_full_n_tokens(
                    context,
                    segmentIndex
            );

    if (tokenIndex < 0 || tokenIndex >= tokenCount) {
        return env->NewStringUTF("");
    }

    const char* text =
            whisper_full_get_token_text(
                    context,
                    segmentIndex,
                    tokenIndex
            );

    if (text == nullptr) {
        return env->NewStringUTF("");
    }

    return env->NewStringUTF(text);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextTokenT0(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong contextPtr,
        jint segmentIndex,
        jint tokenIndex
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return 0;
    }

    return static_cast<jlong>(
            whisper_full_get_token_t0(
                    context,
                    segmentIndex,
                    tokenIndex
            )
    );
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextTokenT1(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong contextPtr,
        jint segmentIndex,
        jint tokenIndex
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return 0;
    }

    return static_cast<jlong>(
            whisper_full_get_token_t1(
                    context,
                    segmentIndex,
                    tokenIndex
            )
    );
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getTextTokenProbability(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong contextPtr,
        jint segmentIndex,
        jint tokenIndex
) {
    whisper_context* context = getContext(contextPtr);

    if (context == nullptr) {
        return 0.0f;
    }

    return whisper_full_get_token_p(
            context,
            segmentIndex,
            tokenIndex
    );
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_whispercpp_java_whisper_WhisperLib_getSystemInfo(
        JNIEnv* env,
        jobject /* thiz */
) {
    const char* info = whisper_print_system_info();

    if (info == nullptr) {
        return env->NewStringUTF("");
    }

    return env->NewStringUTF(info);
}
