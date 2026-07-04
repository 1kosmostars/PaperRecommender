#include "bpr.h"
#include <jni.h>
#include <stdint.h>

/* Helper: cast jlong ↔ pointer */
static inline BprModel* ptr(jlong h) { return (BprModel*)(uintptr_t)h; }

/*
 * Class:     com_recommender_jni_BprNative
 * Method:    createModel
 * Signature: (IIIJ)J
 */
JNIEXPORT jlong JNICALL
Java_com_recommender_jni_BprNative_createModel(
    JNIEnv* env, jclass cls,
    jint numUsers, jint numItems, jint numFactors, jlong seed)
{
    BprModel* m = bpr_create((int)numUsers, (int)numItems, (int)numFactors, (unsigned)seed);
    return (jlong)(uintptr_t)m;
}

/*
 * Class:     com_recommender_jni_BprNative
 * Method:    freeModel
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_recommender_jni_BprNative_freeModel(
    JNIEnv* env, jclass cls, jlong handle)
{
    bpr_free(ptr(handle));
}

/*
 * Class:     com_recommender_jni_BprNative
 * Method:    fit
 * Signature: (J[I[IIIFF)V
 */
JNIEXPORT void JNICALL
Java_com_recommender_jni_BprNative_fit(
    JNIEnv* env, jclass cls,
    jlong handle,
    jintArray jUsers, jintArray jItems, jint nPairs,
    jint numEpochs, jfloat lr, jfloat reg)
{
    BprModel* m = ptr(handle);
    jint* users = (*env)->GetIntArrayElements(env, jUsers, NULL);
    jint* items = (*env)->GetIntArrayElements(env, jItems, NULL);

    bpr_fit(m, (const int*)users, (const int*)items,
            (int)nPairs, (int)numEpochs, (float)lr, (float)reg);

    (*env)->ReleaseIntArrayElements(env, jUsers, users, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, jItems, items, JNI_ABORT);
}

/*
 * Class:     com_recommender_jni_BprNative
 * Method:    recommend
 * Signature: (JI[II)[I
 */
JNIEXPORT jintArray JNICALL
Java_com_recommender_jni_BprNative_recommend(
    JNIEnv* env, jclass cls,
    jlong handle, jint userId,
    jintArray jExclude, jint k)
{
    BprModel* m = ptr(handle);

    jint nExcl  = jExclude ? (*env)->GetArrayLength(env, jExclude) : 0;
    jint* excl  = nExcl > 0
                  ? (*env)->GetIntArrayElements(env, jExclude, NULL)
                  : NULL;

    int actual_k = k < m->num_items ? (int)k : m->num_items;
    jintArray result = (*env)->NewIntArray(env, actual_k);
    jint* out = (*env)->GetIntArrayElements(env, result, NULL);

    bpr_recommend(m, (int)userId, (const int*)excl, (int)nExcl, actual_k, (int*)out);

    if (excl) (*env)->ReleaseIntArrayElements(env, jExclude, excl, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, result, out, 0);
    return result;
}
