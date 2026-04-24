#pragma once

#include <jni.h>

extern "C" JNIEXPORT jint JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_initClamAV(JNIEnv* env, jobject thiz, jstring db_path);

extern "C" JNIEXPORT jstring JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_scanFile(JNIEnv* env, jobject thiz, jstring file_path);

extern "C" JNIEXPORT jstring JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_scanBytes(JNIEnv* env, jobject thiz, jstring name, jbyteArray content);

extern "C" JNIEXPORT jboolean JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_updateDatabase(JNIEnv* env, jobject thiz, jstring url);

extern "C" JNIEXPORT jboolean JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_startSandbox(JNIEnv* env, jobject thiz, jstring packageName);

extern "C" JNIEXPORT jboolean JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_stopSandbox(JNIEnv* env, jobject thiz, jstring packageName);

extern "C" JNIEXPORT jstring JNICALL
Java_com_youravapp_data_jni_ClamAvNativeBridge_analyzeSandbox(JNIEnv* env, jobject thiz, jstring packageName);
