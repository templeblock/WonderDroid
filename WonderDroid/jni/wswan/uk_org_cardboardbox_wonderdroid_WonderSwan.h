/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class uk_org_cardboardbox_wonderdroid_WonderSwan */

#ifndef _Included_uk_org_cardboardbox_wonderdroid_WonderSwan
#define _Included_uk_org_cardboardbox_wonderdroid_WonderSwan
#ifdef __cplusplus
extern "C" {
#endif
#undef uk_org_cardboardbox_wonderdroid_WonderSwan_SCREEN_WIDTH
#define uk_org_cardboardbox_wonderdroid_WonderSwan_SCREEN_WIDTH 224L
#undef uk_org_cardboardbox_wonderdroid_WonderSwan_SCREEN_HEIGHT
#define uk_org_cardboardbox_wonderdroid_WonderSwan_SCREEN_HEIGHT 144L
#undef uk_org_cardboardbox_wonderdroid_WonderSwan_audiobufferlen
#define uk_org_cardboardbox_wonderdroid_WonderSwan_audiobufferlen 4000L
#undef uk_org_cardboardbox_wonderdroid_WonderSwan_channelconf
#define uk_org_cardboardbox_wonderdroid_WonderSwan_channelconf 3L
#undef uk_org_cardboardbox_wonderdroid_WonderSwan_encoding
#define uk_org_cardboardbox_wonderdroid_WonderSwan_encoding 2L
#undef uk_org_cardboardbox_wonderdroid_WonderSwan_audiofreq
#define uk_org_cardboardbox_wonderdroid_WonderSwan_audiofreq 22050L
/*
 * Class:     uk_org_cardboardbox_wonderdroid_WonderSwan
 * Method:    load
 * Signature: (Ljava/lang/String;Z)V
 */
JNIEXPORT void JNICALL Java_uk_org_cardboardbox_wonderdroid_WonderSwan_load
  (JNIEnv *, jclass, jstring, jboolean, jstring, jint, jint, jint, jint, jint);

/*
 * Class:     uk_org_cardboardbox_wonderdroid_WonderSwan
 * Method:    reset
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_org_cardboardbox_wonderdroid_WonderSwan_reset
  (JNIEnv *, jclass);

/*
 * Class:     uk_org_cardboardbox_wonderdroid_WonderSwan
 * Method:    _execute_frame
 * Signature: (ZLjava/nio/ShortBuffer;[S)I
 */
JNIEXPORT jint JNICALL Java_uk_org_cardboardbox_wonderdroid_WonderSwan__1execute_1frame
  (JNIEnv *, jclass, jboolean, jboolean, jobject, jshortArray);

/*
 * Class:     uk_org_cardboardbox_wonderdroid_WonderSwan
 * Method:    updatebuttons
 * Signature: (ZZZZZZZZZZZ)V
 */
JNIEXPORT void JNICALL Java_uk_org_cardboardbox_wonderdroid_WonderSwan_updatebuttons
  (JNIEnv *, jclass, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean);

/*
 * Class:     uk_org_cardboardbox_wonderdroid_WonderSwan
 * Method:    storebackupdata
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_uk_org_cardboardbox_wonderdroid_WonderSwan_storebackupdata
  (JNIEnv *, jclass, jstring);

/*
 * Class:     uk_org_cardboardbox_wonderdroid_WonderSwan
 * Method:    loadbackupdata
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_uk_org_cardboardbox_wonderdroid_WonderSwan_loadbackupdata
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
