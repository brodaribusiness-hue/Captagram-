cmake_minimum_required(VERSION 3.22.1)

project("captagram")

add_subdirectory(whisper)

add_library(
        captagram
        SHARED
        native-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        captagram
        ${log-lib}
)
