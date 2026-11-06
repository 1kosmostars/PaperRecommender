cmake_minimum_required(VERSION 3.20)
project(recommender_native C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

if (MSVC)
    add_compile_options(/W3 /O2)
else()
    add_compile_options(-Wall -O3 -ffast-math)
endif()

# ── HNSW ──────────────────────────────────────────────────────────────────────
add_library(hnsw SHARED
    hnsw/hnsw.c
    hnsw/hnsw_jni.c
)
target_include_directories(hnsw PRIVATE hnsw/)
if (NOT MSVC)
    target_link_libraries(hnsw m)
endif()

# ── PageRank ──────────────────────────────────────────────────────────────────
add_library(pagerank SHARED
    pagerank/pagerank.c
    pagerank/pagerank_jni.c
)
target_include_directories(pagerank PRIVATE pagerank/)

# ── BPR ───────────────────────────────────────────────────────────────────────
add_library(bpr SHARED
    bpr/bpr.c
    bpr/bpr_jni.c
)
target_include_directories(bpr PRIVATE bpr/)
if (NOT MSVC)
    target_link_libraries(bpr m)
else()
    # MSVC: expf is in the CRT, no separate -lm needed
endif()

# ── Copy all libs to resources/native/ after build ────────────────────────────
set(RESOURCES_DIR "${CMAKE_SOURCE_DIR}/../src/main/resources/native")

foreach(LIB hnsw pagerank bpr)
    add_custom_command(TARGET ${LIB} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory "${RESOURCES_DIR}"
        COMMAND ${CMAKE_COMMAND} -E copy
            "$<TARGET_FILE:${LIB}>"
            "${RESOURCES_DIR}/$<TARGET_FILE_NAME:${LIB}>"
        COMMENT "Copying ${LIB} to resources/native/"
    )
endforeach()
