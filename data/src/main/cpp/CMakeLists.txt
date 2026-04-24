cmake_minimum_required(VERSION 3.22.1)
project(clamav_jni)

add_library(clamav_jni SHARED native-lib.cpp)

target_include_directories(clamav_jni PRIVATE ${CMAKE_SOURCE_DIR}/clamav/include)

find_library(log-lib log)

target_link_libraries(clamav_jni ${log-lib})
