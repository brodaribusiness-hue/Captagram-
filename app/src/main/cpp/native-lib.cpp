#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_brodari_captagram_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    return env->NewStringUTF("Captagram native layer");
}
