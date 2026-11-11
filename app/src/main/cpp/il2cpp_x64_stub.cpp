#include <android/log.h>

#if !defined(__x86_64__) && !defined(__aarch64__)
#error "libil2cpp target must be arm64-v8a or x86_64!"
#endif

extern "C" int ror2_il2cpp_entry(void) {
    __android_log_print(ANDROID_LOG_INFO, "RoR2_IL2CPP", "libil2cpp.so (arm64-v8a / x86_64) initialized");
    return 0;
}
