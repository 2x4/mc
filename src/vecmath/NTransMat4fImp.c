#include <math.h>
#include "vecmath_NTransMat4f.h"

/*
 * Class:     vecmath_NTransMat4f
 * Method:    transform
 * Signature: (Lvecmath/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_vecmath_NTransMat4f_transform(JNIEnv *env, jobject obj, jobject vec3f)
{
  jclass clazz = (*env)->GetObjectClass(env, vec3f);
  jclass thisclass = (*env)->GetObjectClass(env, obj);

  jfieldID fid1 = (*env)->GetFieldID(env, clazz, "x", "F");
  jfieldID fid2 = (*env)->GetFieldID(env, clazz, "y", "F");
  jfieldID fid3 = (*env)->GetFieldID(env, clazz, "z", "F");
  jfloat x = (*env)->GetFloatField(env, vec3f, fid1);
  jfloat y = (*env)->GetFloatField(env, vec3f, fid2);
  jfloat z = (*env)->GetFloatField(env, vec3f, fid3);

  jfieldID fid00 = (*env)->GetFieldID(env, thisclass, "m00", "F");
  jfieldID fid01 = (*env)->GetFieldID(env, thisclass, "m01", "F");
  jfieldID fid02 = (*env)->GetFieldID(env, thisclass, "m02", "F");
  jfieldID fid10 = (*env)->GetFieldID(env, thisclass, "m10", "F");
  jfieldID fid11 = (*env)->GetFieldID(env, thisclass, "m11", "F");
  jfieldID fid12 = (*env)->GetFieldID(env, thisclass, "m12", "F");
  jfieldID fid20 = (*env)->GetFieldID(env, thisclass, "m20", "F");
  jfieldID fid21 = (*env)->GetFieldID(env, thisclass, "m21", "F");
  jfieldID fid22 = (*env)->GetFieldID(env, thisclass, "m22", "F");

  jfloat m00 = (*env)->GetFloatField(env, obj, fid00);
  jfloat m01 = (*env)->GetFloatField(env, obj, fid01);
  jfloat m02 = (*env)->GetFloatField(env, obj, fid02);
  jfloat m10 = (*env)->GetFloatField(env, obj, fid10);
  jfloat m11 = (*env)->GetFloatField(env, obj, fid11);
  jfloat m12 = (*env)->GetFloatField(env, obj, fid12);
  jfloat m20 = (*env)->GetFloatField(env, obj, fid20);
  jfloat m21 = (*env)->GetFloatField(env, obj, fid21);
  jfloat m22 = (*env)->GetFloatField(env, obj, fid22);

  float vx = m00*x + m01*y + m02*z;
  float vy = m10*x + m11*y + m12*z;
  float vz = m20*x + m21*y + m22*z;
  //  printf("transform(): %f %f %f\n", x, y, z);
  //  printf("transform(): %f %f %f\n", vx, vy, vz);

  (*env)->SetFloatField(env, vec3f, fid1, vx);
  (*env)->SetFloatField(env, vec3f, fid2, vy);
  (*env)->SetFloatField(env, vec3f, fid3, vz);

  /*
  jmethodID mid = (*env)->GetMethodID(env, clazz, "toString", "()Ljava/lang/String;");
  jstring name = (jstring)(*env)->CallObjectMethod(env, obj, mid);
  const char * buff;
  jboolean isCopy;
  buff = (*env)->GetStringUTFChars(env, name, &isCopy);
  printf("%s\n", buff);
  if (isCopy == JNI_TRUE) {
    (*env)->ReleaseStringChars(env, name, buff);
  }
  */
}

/*
 * Class:     vecmath_NTransMat4f
 * Method:    setRotation
 * Signature: (FFF)V
 */
JNIEXPORT void JNICALL Java_vecmath_NTransMat4f_setRotation(JNIEnv *env, jobject obj, jfloat pitch, jfloat roll, jfloat yaw)
{
  jclass clazz = (*env)->GetObjectClass(env, obj);
  jfieldID m00 = (*env)->GetFieldID(env, clazz, "m00", "F");
  jfieldID m01 = (*env)->GetFieldID(env, clazz, "m01", "F");
  jfieldID m02 = (*env)->GetFieldID(env, clazz, "m02", "F");
  jfieldID m03 = (*env)->GetFieldID(env, clazz, "m03", "F");
  jfieldID m10 = (*env)->GetFieldID(env, clazz, "m10", "F");
  jfieldID m11 = (*env)->GetFieldID(env, clazz, "m11", "F");
  jfieldID m12 = (*env)->GetFieldID(env, clazz, "m12", "F");
  jfieldID m13 = (*env)->GetFieldID(env, clazz, "m13", "F");
  jfieldID m20 = (*env)->GetFieldID(env, clazz, "m20", "F");
  jfieldID m21 = (*env)->GetFieldID(env, clazz, "m21", "F");
  jfieldID m22 = (*env)->GetFieldID(env, clazz, "m22", "F");
  jfieldID m23 = (*env)->GetFieldID(env, clazz, "m23", "F");
  jfieldID m30 = (*env)->GetFieldID(env, clazz, "m30", "F");
  jfieldID m31 = (*env)->GetFieldID(env, clazz, "m31", "F");
  jfieldID m32 = (*env)->GetFieldID(env, clazz, "m32", "F");
  jfieldID m33 = (*env)->GetFieldID(env, clazz, "m33", "F");

  float sinx = (float)sin((double)roll);
  float cosx = (float)cos((double)roll);
  float siny = (float)sin((double)pitch);
  float cosy = (float)cos((double)pitch);
  float sinz = (float)sin((double)yaw);
  float cosz = (float)cos((double)yaw);

  printf("setRotation(): yaw=%f, roll=%f, pitch=%f\n", (float)yaw, (float)roll, (float)pitch);

  (*env)->SetFloatField(env, obj, m00, cosz * cosy);
  (*env)->SetFloatField(env, obj, m01, -sinz * cosx + cosz * siny * sinx);
  (*env)->SetFloatField(env, obj, m02, sinz * sinx + cosz * siny * cosx);
  (*env)->SetFloatField(env, obj, m03, 0.0F);
  (*env)->SetFloatField(env, obj, m10, sinz * cosy);
  (*env)->SetFloatField(env, obj, m11, cosz * cosx + sinz * siny * sinx);
  (*env)->SetFloatField(env, obj, m12, -cosz * sinx + sinz * siny * cosx);
  (*env)->SetFloatField(env, obj, m13, 0.0F);
  (*env)->SetFloatField(env, obj, m20, -siny);
  (*env)->SetFloatField(env, obj, m21, cosy * sinx);
  (*env)->SetFloatField(env, obj, m22, cosy * cosx);
  (*env)->SetFloatField(env, obj, m23, 0.0F);
  (*env)->SetFloatField(env, obj, m30, 0.0F);
  (*env)->SetFloatField(env, obj, m31, 0.0F);
  (*env)->SetFloatField(env, obj, m32, 0.0F);
  (*env)->SetFloatField(env, obj, m33, 1.0F);
}

/*
 * Class:     vecmath_NTransMat4f
 * Method:    mul
 * Signature: (Lvecmath/TransMat4f;)V
 */

JNIEXPORT void JNICALL Java_vecmath_NTransMat4f_mul(JNIEnv *env, jobject obj, jobject t);

/*
 * Class:     vecmath_NTransMat4f
 * Method:    getEuler
 * Signature: ()Lvecmath/Euler;
 */
JNIEXPORT jobject JNICALL Java_vecmath_NTransMat4f_getEuler (JNIEnv *env, jobject obj)
{
  jclass clazz = (*env)->GetObjectClass(env, obj);

  jclass clsEuler  = (*env)->FindClass(env, "vecmath/Euler");
  jmethodID constructor = (*env)->GetMethodID(env, clsEuler, "<init>", "()V");
  jobject objEuler = (*env)->NewObject(env, clsEuler, constructor);

  jfieldID fid00 = (*env)->GetFieldID(env, clazz, "m00", "F");
  jfieldID fid10 = (*env)->GetFieldID(env, clazz, "m10", "F");
  jfieldID fid20 = (*env)->GetFieldID(env, clazz, "m20", "F");
  jfieldID fid21 = (*env)->GetFieldID(env, clazz, "m21", "F");
  jfieldID fid22 = (*env)->GetFieldID(env, clazz, "m22", "F");

  jfloat m00 = (*env)->GetFloatField(env, obj, fid00);
  jfloat m10 = (*env)->GetFloatField(env, obj, fid10);
  jfloat m20 = (*env)->GetFloatField(env, obj, fid20);
  jfloat m21 = (*env)->GetFloatField(env, obj, fid21);
  jfloat m22 = (*env)->GetFloatField(env, obj, fid22);

  jfieldID fidp, fidr, fidy;
  //  printf("m00=%f, m10=%f, m20=%f, m21=%f, m22=%f\n", m00, m10, m20, m21, m22);
  fidp = (*env)->GetFieldID(env, clsEuler, "pitch", "F");
  fidr = (*env)->GetFieldID(env, clsEuler, "roll", "F");
  fidy = (*env)->GetFieldID(env, clsEuler, "yaw", "F");

  (*env)->SetFloatField(env, objEuler, fidp, asin(-m20));
  (*env)->SetFloatField(env, objEuler, fidr, atan2(m21, m22));
  (*env)->SetFloatField(env, objEuler, fidy, atan2(m10, m00));

  //  printf("getEuler(): pitch = %f, roll = %f, yaw = %f\n", 

  /*
  jmethodID mid;
  jstring name;
  jboolean isCopy;
  const char *buff;


  mid = (*env)->GetMethodID(env, objEuler, "toString", "()Ljava/lang/String;");
  name = (jstring)(*env)->CallObjectMethod(env, objEuler, mid);
  buff = (*env)->GetStringUTFChars(env, name, &isCopy);
  printf("toString() = %s\n", buff);
  */
  return objEuler;
}
