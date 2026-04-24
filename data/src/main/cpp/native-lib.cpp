#include "clamav-jni.h"
#include <android/log.h>
#include <fstream>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

#define LOG_TAG "TrinityAV"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::mutex g_lock;
std::vector<std::string> g_signatures = {
        "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*",
        "meterpreter",
        "njrat",
        "darkcomet",
        "wannacry",
        "locky"
};

std::string check_payload(const std::string& payload) {
    for (const auto& sig : g_signatures) {
        if (!sig.empty() && payload.find(sig) != std::string::npos) {
            return "CL_VIRUS:" + sig;
        }
    }
    if (payload.find(".locked") != std::string::npos || payload.find("readme_decrypt") != std::string::npos) {
        return "CL_VIRUS:Ransomware-Behavior-Heuristic";
    }
    return "CL_CLEAN";
}

void load_custom_signatures(const std::string& path) {
    std::ifstream file(path);
    if (!file.is_open()) {
        LOGI("Signature file not found at %s, using built-in signatures", path.c_str());
        return;
    }
    std::string line;
    while (std::getline(file, line)) {
        if (!line.empty()) g_signatures.push_back(line);
    }
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_initClamAV(JNIEnv* env, jobject, jstring db_path) {
    const char* rawPath = env->GetStringUTFChars(db_path, nullptr);
    std::string path(rawPath ? rawPath : "");
    env->ReleaseStringUTFChars(db_path, rawPath);

    std::lock_guard<std::mutex> guard(g_lock);
    load_custom_signatures(path + "/signatures.db");
    LOGI("Scanner initialized with %zu signatures", g_signatures.size());
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_scanFile(JNIEnv* env, jobject, jstring file_path) {
    const char* rawPath = env->GetStringUTFChars(file_path, nullptr);
    std::string path(rawPath ? rawPath : "");
    env->ReleaseStringUTFChars(file_path, rawPath);

    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return env->NewStringUTF("CL_CLEAN");
    }

    std::ostringstream ss;
    ss << file.rdbuf();
    std::string payload = ss.str();

    std::lock_guard<std::mutex> guard(g_lock);
    auto result = check_payload(payload);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_scanBytes(JNIEnv* env, jobject, jstring name, jbyteArray content) {
    (void)name;
    jsize len = env->GetArrayLength(content);
    std::string payload(static_cast<size_t>(len), '\0');
    env->GetByteArrayRegion(content, 0, len, reinterpret_cast<jbyte*>(&payload[0]));

    std::lock_guard<std::mutex> guard(g_lock);
    auto result = check_payload(payload);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_updateDatabase(JNIEnv* env, jobject, jstring url) {
    const char* rawUrl = env->GetStringUTFChars(url, nullptr);
    std::string source(rawUrl ? rawUrl : "");
    env->ReleaseStringUTFChars(url, rawUrl);

    const bool valid = source.rfind("https://", 0) == 0;
    if (!valid) {
        LOGE("Rejected DB URL: %s", source.c_str());
    }
    return valid ? JNI_TRUE : JNI_FALSE;
}
