cmake_minimum_required(VERSION 3.22.1)
project("ror2_dual_arch_port" LANGUAGES C CXX)

if(NOT ANDROID_ABI STREQUAL "x86_64" AND NOT ANDROID_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "Supported ABIs are arm64-v8a and x86_64, got: ${ANDROID_ABI}")
endif()

add_library(ror2_x64_bridge SHARED ror2_x64_bridge.cpp)
add_library(il2cpp SHARED il2cpp_x64_stub.cpp)

find_library(log-lib log)
target_link_libraries(ror2_x64_bridge ${log-lib})
target_link_libraries(il2cpp ${log-lib})
