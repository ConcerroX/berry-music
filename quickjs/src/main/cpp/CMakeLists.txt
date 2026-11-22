cmake_minimum_required(VERSION 3.22.1)

project(quickjs-ng C CXX)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -fvisibility=hidden -flto -fdata-sections -ffunction-sections -fomit-frame-pointer")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fvisibility=hidden -flto -fdata-sections -ffunction-sections -fomit-frame-pointer")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -flto -Wl,--exclude-libs,ALL -Wl,--gc-sections -s -Wl,--pack-dyn-relocs=android -Wl,--build-id=none -Wl,-z,max-page-size=16384")

add_library(quickjs-ng SHARED
        quickjs_bridge.cpp
        quickjs-ng/quickjs.c
        quickjs-ng/libregexp.c
        quickjs-ng/libunicode.c
        quickjs-ng/dtoa.c)

target_include_directories(quickjs-ng PRIVATE quickjs-ng)
target_compile_definitions(quickjs-ng PRIVATE CONFIG_VERSION="0.14.0")
target_link_libraries(quickjs-ng PRIVATE android log)

add_custom_command(TARGET quickjs-ng POST_BUILD
        COMMAND ${CMAKE_OBJCOPY}
        --remove-section .comment
        --remove-section .note
        --strip-debug $<TARGET_FILE:quickjs-ng>)
