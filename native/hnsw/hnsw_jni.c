#include "hnsw.h"
#include <jni.h>
#include <string.h>

/*
 * JNI bridge for HnswIndex.
 * Java class: com.recommender.jni.HnswNative
 *
 * Convention: the native pointer is passed as jlong (pointer cast to int64).
 */

#define IDX(ptr) ((HnswIndex *)(intptr_t)(ptr))

/* long createIndex(int dim, int M, int efConstruction) */
JNIEXPORT jlong JNICALL
Java_com_recommender_jni_HnswNative_createIndex(JNIEnv *env, jclass cls,
                                                 jint dim, jint M, jint ef) {
    (void)env; (void)cls;
    return (jlong)(intptr_t)hnsw_create((int)dim, (int)M, (int)ef);
}

/* void freeIndex(long ptr) */
JNIEXPORT void JNICALL
Java_com_recommender_jni_HnswNative_freeIndex(JNIEnv *env, jclass cls, jlong ptr) {
    (void)env; (void)cls;
    hnsw_free(IDX(ptr));
}

/* int addVector(long ptr, float[] vector) */
JNIEXPORT jint JNICALL
Java_com_recommender_jni_HnswNative_addVector(JNIEnv *env, jclass cls,
                                               jlong ptr, jfloatArray vec) {
    (void)cls;
    HnswIndex *idx = IDX(ptr);
    jfloat *data = (*env)->GetFloatArrayElements(env, vec, NULL);
    int id = hnsw_add(idx, (float *)data);
    (*env)->ReleaseFloatArrayElements(env, vec, data, JNI_ABORT);
    return (jint)id;
}

/* int[] search(long ptr, float[] query, int k, int ef) */
JNIEXPORT jintArray JNICALL
Java_com_recommender_jni_HnswNative_search(JNIEnv *env, jclass cls,
                                            jlong ptr, jfloatArray query,
                                            jint k, jint ef) {
    (void)cls;
    HnswIndex *idx = IDX(ptr);

    jfloat *qdata = (*env)->GetFloatArrayElements(env, query, NULL);
    int *out_ids   = (int *)malloc((size_t)k * sizeof(int));
    float *out_d   = (float *)malloc((size_t)k * sizeof(float));

    int n = hnsw_search(idx, (float *)qdata, (int)k, (int)ef, out_ids, out_d);
    (*env)->ReleaseFloatArrayElements(env, query, qdata, JNI_ABORT);
    free(out_d);

    jintArray result = (*env)->NewIntArray(env, (jsize)n);
    (*env)->SetIntArrayRegion(env, result, 0, (jsize)n, (jint *)out_ids);
    free(out_ids);
    return result;
}

/* float[] searchWithDistances(long ptr, float[] query, int k, int ef) */
/* Returns interleaved [id0, dist0_bits, id1, dist1_bits, ...] as int pairs */
JNIEXPORT jfloatArray JNICALL
Java_com_recommender_jni_HnswNative_searchWithDistances(JNIEnv *env, jclass cls,
                                                          jlong ptr, jfloatArray query,
                                                          jint k, jint ef) {
    (void)cls;
    HnswIndex *idx = IDX(ptr);

    jfloat *qdata  = (*env)->GetFloatArrayElements(env, query, NULL);
    int    *out_ids = (int *)  malloc((size_t)k * sizeof(int));
    float  *out_d   = (float *)malloc((size_t)k * sizeof(float));

    int n = hnsw_search(idx, (float *)qdata, (int)k, (int)ef, out_ids, out_d);
    (*env)->ReleaseFloatArrayElements(env, query, qdata, JNI_ABORT);

    /* Return float[] of length n; caller casts every other element back to int for ids */
    jfloatArray result = (*env)->NewFloatArray(env, (jsize)(n * 2));
    jfloat *buf = (*env)->GetFloatArrayElements(env, result, NULL);
    for (int i = 0; i < n; i++) {
        /* Store id as float bits */
        memcpy(&buf[i * 2],     &out_ids[i], sizeof(float));
        buf[i * 2 + 1] = out_d[i];
    }
    (*env)->ReleaseFloatArrayElements(env, result, buf, 0);
    free(out_ids); free(out_d);
    return result;
}

/* int size(long ptr) */
JNIEXPORT jint JNICALL
Java_com_recommender_jni_HnswNative_size(JNIEnv *env, jclass cls, jlong ptr) {
    (void)env; (void)cls;
    return (jint)IDX(ptr)->size;
}
