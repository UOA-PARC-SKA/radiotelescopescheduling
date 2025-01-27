/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ext_NovasLink */

#ifndef _Included_ext_NovasLink
#define _Included_ext_NovasLink
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ext_NovasLink
 * Method:    getCBHorizonCoordinates
 * Signature: (JILobservation/Telescope;)Lastrometrics/HorizonCoordinates;
 */
JNIEXPORT jobject JNICALL Java_ext_NovasLink_getCBHorizonCoordinates
  (JNIEnv *, jobject, jlong, jint, jobject);

/*
 * Class:     ext_NovasLink
 * Method:    getCBRadius
 * Signature: (JILobservation/Telescope;)D
 */
JNIEXPORT jdouble JNICALL Java_ext_NovasLink_getCBRadius
  (JNIEnv *, jobject, jlong, jint, jobject);

#ifdef __cplusplus
}
#endif
#endif
