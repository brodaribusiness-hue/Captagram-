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

target_sources(
        whisper
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/whisper-jni.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        whisper
        PRIVATE
        ${log-lib}
        android
)

add_library(
        captagram
        SHARED
        native-lib.cpp
)

target_link_libraries(
        captagram
        ${log-lib}
)
