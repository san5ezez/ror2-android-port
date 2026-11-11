#include <jni.h>
#include <android/log.h>
#include <sys/utsname.h>
#include <string>

#define LOG_TAG "RoR2_Native_Bridge"

#if defined(__x86_64__)
#define ROR2_TARGET_ABI "x86_64"
#elif defined(__aarch64__)
#define ROR2_TARGET_ABI "arm64-v8a"
#else
#error "Target architecture must be arm64-v8a or x86_64!"
#endif

extern "C" JNIEXPORT jstring JNICALL
Java_dev_ror2owner_bootstrap_PayloadBootstrapActivity_getNativeArchInfo(JNIEnv* env, jclass) {
    struct utsname buffer{};
    uname(&buffer);
    std::string info = std::string("ABI=") + ROR2_TARGET_ABI + "; machine=" + buffer.machine + "; release=" + buffer.release;
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Native bridge active: %s", info.c_str());
    return env->NewStringUTF(info.c_str());
}
