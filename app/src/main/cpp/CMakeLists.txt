cmake_minimum_required(VERSION 3.22.1)

project("captagram")

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_CURL OFF CACHE BOOL "" FORCE)
set(WHISPER_SDL2 OFF CACHE BOOL "" FORCE)

add_subdirectory(
        ${CMAKE_CURRENT_SOURCE_DIR}/whisper
)

add_library(
        captagram
        SHARED
        native-lib.cpp
)

add_library(
        captagram_whisper
        SHARED
        whisper-jni.cpp
)

find_library(
        log-lib
        log
)

target_include_directories(
        captagram_whisper
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/whisper/include
        ${CMAKE_CURRENT_SOURCE_DIR}/whisper/ggml/include
)

target_link_libraries(
        captagram
        ${log-lib}
)

target_link_libraries(
        captagram_whisper
        whisper
        ${log-lib}
        android
)
