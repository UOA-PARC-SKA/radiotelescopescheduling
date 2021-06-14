#ifndef _NORAD_LINK_IMPL_H
#define _NORAD_LINK_IMPL_H

#ifdef __cplusplus
        extern "C" {
#endif
        jobject getSatHorizonCoordinates (JNIEnv *env, jobject satellite, long int datetime, jobject telescope);
#ifdef __cplusplus
        }
#endif

#endif
