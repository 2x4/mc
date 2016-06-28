#include <jni.h>
/* Header for class vecmath_NTransMat4f */

#ifndef _Included_vecmath_NTransMat4f
#define _Included_vecmath_NTransMat4f
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     vecmath_NTransMat4f
 * Method:    transform
 * Signature: (Lvecmath/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_vecmath_NTransMat4f_transform
  (JNIEnv *, jobject, jobject);

/*
 * Class:     vecmath_NTransMat4f
 * Method:    setRotation
 * Signature: (FFF)V
 */
JNIEXPORT void JNICALL Java_vecmath_NTransMat4f_setRotation
  (JNIEnv *, jobject, jfloat, jfloat, jfloat);

/*
 * Class:     vecmath_NTransMat4f
 * Method:    mul
 * Signature: (Lvecmath/TransMat4f;)V
 */
JNIEXPORT void JNICALL Java_vecmath_NTransMat4f_mul
  (JNIEnv *, jobject, jobject);

/*
 * Class:     vecmath_NTransMat4f
 * Method:    getEuler
 * Signature: ()Lvecmath/Euler;
 */
JNIEXPORT jobject JNICALL Java_vecmath_NTransMat4f_getEuler
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
