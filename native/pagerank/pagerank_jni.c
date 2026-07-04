#include "pagerank.h"
#include <jni.h>
#include <stdlib.h>

/*
 * Class:     com_recommender_jni_PageRankNative
 * Method:    compute
 * Signature: ([I[IIDId)[D
 */
JNIEXPORT jdoubleArray JNICALL
Java_com_recommender_jni_PageRankNative_compute(
    JNIEnv* env, jclass cls,
    jintArray jCiting, jintArray jCited,
    jint numEdges, jint numNodes,
    jdouble damping, jint iterations)
{
    if (numNodes <= 0) return NULL;

    jint*    citing = (*env)->GetIntArrayElements(env, jCiting, NULL);
    jint*    cited  = (*env)->GetIntArrayElements(env, jCited,  NULL);

    jdoubleArray result = (*env)->NewDoubleArray(env, numNodes);
    jdouble*     scores = (*env)->GetDoubleArrayElements(env, result, NULL);

    pagerank_compute(
        (const int*)citing, (const int*)cited,
        (int)numEdges, (int)numNodes,
        (double)damping, (int)iterations,
        (double*)scores
    );

    (*env)->ReleaseIntArrayElements(env, jCiting, citing, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, jCited,  cited,  JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, result, scores, 0);

    return result;
}
