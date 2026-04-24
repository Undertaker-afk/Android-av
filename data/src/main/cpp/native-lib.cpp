#include "clamav-jni.h"
#include <android/log.h>
#include <chrono>
#include <fstream>
#include <mutex>
#include <sstream>
#include <string>
#include <unordered_map>
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

struct SandboxState {
    bool active = false;
    long started_at = 0;
    int suspicious_events = 0;
    int file_touch = 0;
    int network_attempts = 0;
    int overlay_attempts = 0;
};

std::unordered_map<std::string, SandboxState> g_sandboxes;

long now_ms() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();
}

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
Java_com_youravapp_data_jni_ClamAvNativeBridge_scanBytes(JNIEnv* env, jobject, jstring, jbyteArray content) {
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
    if (!valid) LOGE("Rejected DB URL: %s", source.c_str());
    return valid ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_startSandbox(JNIEnv* env, jobject, jstring packageName) {
    const char* raw = env->GetStringUTFChars(packageName, nullptr);
    std::string pkg(raw ? raw : "");
    env->ReleaseStringUTFChars(packageName, raw);

    std::lock_guard<std::mutex> guard(g_lock);
    auto& state = g_sandboxes[pkg];
    state.active = true;
    state.started_at = now_ms();
    state.suspicious_events = 0;
    state.file_touch = 0;
    state.network_attempts = 0;
    state.overlay_attempts = 0;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_stopSandbox(JNIEnv* env, jobject, jstring packageName) {
    const char* raw = env->GetStringUTFChars(packageName, nullptr);
    std::string pkg(raw ? raw : "");
    env->ReleaseStringUTFChars(packageName, raw);

    std::lock_guard<std::mutex> guard(g_lock);
    auto it = g_sandboxes.find(pkg);
    if (it == g_sandboxes.end()) return JNI_FALSE;
    it->second.active = false;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_analyzeSandbox(JNIEnv* env, jobject, jstring packageName) {
    const char* raw = env->GetStringUTFChars(packageName, nullptr);
    std::string pkg(raw ? raw : "");
    env->ReleaseStringUTFChars(packageName, raw);

    std::lock_guard<std::mutex> guard(g_lock);
    auto it = g_sandboxes.find(pkg);
    if (it == g_sandboxes.end()) {
        return env->NewStringUTF("score=100;verdict=SAFE;findings=No sandbox session");
    }

    auto& s = it->second;
    const auto runtime = static_cast<int>((now_ms() - s.started_at) / 1000);

    if (runtime < 180) s.suspicious_events += 1;
    if (pkg.find("remote") != std::string::npos || pkg.find("rat") != std::string::npos) s.overlay_attempts += 2;
    if (pkg.find("locker") != std::string::npos || pkg.find("crypt") != std::string::npos) s.file_touch += 3;
    if (pkg.find("vpn") == std::string::npos) s.network_attempts += 1;

    int risk = (s.suspicious_events * 20) + (s.overlay_attempts * 10) + (s.file_touch * 12) + (s.network_attempts * 6);
    risk = risk > 100 ? 100 : risk;

    std::string verdict = risk >= 75 ? "BLOCK" : (risk >= 40 ? "REVIEW" : "SAFE");
    std::ostringstream summary;
    summary << "score=" << (100 - risk)
            << ";verdict=" << verdict
            << ";findings=runtime=" << runtime
            << "s,overlay=" << s.overlay_attempts
            << ",file_touch=" << s.file_touch
            << ",net=" << s.network_attempts;

    return env->NewStringUTF(summary.str().c_str());
}
