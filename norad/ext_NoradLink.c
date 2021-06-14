#include <jni.h>
#include "ext_NoradLink.h"
#include "NoradLinkImpl.h"

JNIEXPORT jobject JNICALL Java_ext_NoradLink_getSatHorizonCoordinates (JNIEnv *env, jobject thisObj, jobject satellite, jlong time, jobject telescope)
{


	long int datetime = (long int)time;

    return getSatHorizonCoordinates(env, satellite, datetime, telescope);
}
