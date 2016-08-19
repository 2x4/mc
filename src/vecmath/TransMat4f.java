package vecmath;

/**
 * 回転行列をあらわす4x4行列
 */
public class TransMat4f {
  float m00, m01, m02, m03;
  float m10, m11, m12, m13;
  float m20, m21, m22, m23;
  float m30, m31, m32, m33;
    
  public TransMat4f() {
    /*
      m00 = 1.0;       m01 = 0.0;       m02 = 0.0;       m03 = 0.0;
      m10 = 0.0;       m11 = 1.0;       m12 = 0.0;       m13 = 0.0;
      m20 = 0.0;       m21 = 0.0;       m22 = 1.0;       m23 = 0.0;
      m30 = 0.0;       m31 = 0.0;       m32 = 0.0;       m33 = 1.0;
    */
  }

//  public TransMat4f(TransMat4f t) {
//    m00 = t.m00;    m01 = t.m01;    m02 = t.m02;    m03 = t.m03;
//    m10 = t.m10;    m11 = t.m11;    m12 = t.m12;    m13 = t.m13;
//    m20 = t.m20;    m21 = t.m21;    m22 = t.m22;    m23 = t.m23;
//    m30 = t.m30;    m31 = t.m31;    m32 = t.m32;    m33 = t.m33;
//  }

//  public TransMat4f(Euler rot) {
//    this.setRotation(rot);
//  }

  /**
   * この行列とベクタvを乗算して，その結果をベクタvに保存する．
   */
  public final void transform(Vector3f v) {
    float vx = v.x;
    float vy = v.y;
    float vz = v.z;

    v.x = m00 * vx + m01 * vy + m02 * vz;
    v.y = m10 * vx + m11 * vy + m12 * vz;
    v.z = m20 * vx + m21 * vy + m22 * vz;
  }

  public final void setRevRotation(Euler e) {
    setRevRotation(e.getPitchRad(), e.getRollRad(), e.getYawRad());
  }

  /**
   * この行列の回転を-rot.z, -rot.y, -rot.xの順に回転するように設定する．
   */
  public final void setRevRotation(float pitch, float roll, float yaw) {
    /*
    float sinx = Math.sin(roll);
    float cosx = Math.cos(roll);
    float siny = Math.sin(pitch);
    float cosy = Math.cos(pitch);
    float sinz = Math.sin(yaw);
    float cosz = Math.cos(yaw);
    m00 = cosy * cosz;
    m01 = cosy * sinz;
    m02 = -siny;
    m10 = sinx * siny * cosz - cosx * sinz;
    m11 = sinx * siny * sinz + cosx * cosz;
    m12 = sinx * cosy;
    m20 = cosx * siny * cosz + sinx * sinz;
    m21 = cosz * siny * sinz - sinx * cosz;
    m22 = cosx * cosy;
    */
    setRotation(pitch, roll, yaw);
    transpose();
  }

  public final void transpose() {
    float a01 = m01;
    float a02 = m02;
    float a10 = m10;
    float a12 = m12;
    float a20 = m20;
    float a21 = m21;
    m01 = a10;
    m02 = a20;
    m10 = a01;
    m12 = a21;
    m20 = a02;
    m21 = a12;
  }

  public final void setRotation(Euler e) {
    setRotation(e.getPitchRad(), e.getRollRad(), e.getYawRad());
  }

  /**
   * この行列の回転をrot.x, rot.y, rot.zの順に回転するように設定する．
   */
  public final void setRotation(float pitch, float roll, float yaw) {
    float sinx = (float)Math.sin((double)roll);
    float cosx = (float)Math.cos((double)roll);
    float siny = (float)Math.sin((double)pitch);
    float cosy = (float)Math.cos((double)pitch);
    float sinz = (float)Math.sin((double)yaw);
    float cosz = (float)Math.cos((double)yaw);
       
    m00 = cosz * cosy;
    m01 = -sinz * cosx + cosz * siny * sinx;
    m02 = sinz * sinx + cosz * siny * cosx;
    m03 = 0.0F;
       
    m10 = sinz * cosy;
    m11 = cosz * cosx + sinz * siny * sinx;
    m12 = -cosz * sinx + sinz * siny * cosx;
    m13 = 0.0F;
       
    m20 = -siny;
    m21 = cosy * sinx;
    m22 = cosy * cosx;
    m23 = 0.0F;
       
    m30 = 0.0F;
    m31 = 0.0F;
    m32 = 0.0F;
    m33 = 1.0F;
  }

  public final void mul(TransMat4f t) {
    float a00 = m00;
    float a01 = m01;
    float a02 = m02;
    float a10 = m10;
    float a11 = m11;
    float a12 = m12;
    float a20 = m20;
    float a21 = m21;
    float a22 = m22;
    m00 = a00 * t.m00 + a01 * t.m10 + a02 * t.m20;
    m01 = a00 * t.m01 + a01 * t.m11 + a02 * t.m21;
    m02 = a00 * t.m02 + a01 * t.m12 + a02 * t.m22;
    m10 = a10 * t.m00 + a11 * t.m10 + a12 * t.m20;
    m11 = a10 * t.m01 + a11 * t.m11 + a12 * t.m21;
    m12 = a10 * t.m02 + a11 * t.m12 + a12 * t.m22;
    m20 = a20 * t.m00 + a21 * t.m10 + a22 * t.m20;
    m21 = a20 * t.m01 + a21 * t.m11 + a22 * t.m21;
    m22 = a20 * t.m02 + a21 * t.m12 + a22 * t.m22;
  }

  public final Euler getEuler() {
    Euler e = new Euler();
    e.setPitchRad((float)Math.asin(-m20));
    e.setRollRad((float)Math.atan2(m21, m22));
    e.setYawRad((float)Math.atan2(m10, m00));
    return e;
  }
/*
  public static void main(String args[]) {
      float DEG2RAD = (float)(Math.PI / 180.0);
      float pitch = 30.0f;
      float roll = 45.0f;
      float yaw = 60.0f;
      //      NTransMat4f t = new NTransMat4f(new Euler(pitch, roll, yaw));
      TransMat4f t = new TransMat4f();
      t.setRotation(pitch*DEG2RAD, roll*DEG2RAD, yaw*DEG2RAD);
      System.out.println(t.toString());
      Euler e = t.getEuler();
      System.out.println(e.toString());
      Vector3f p = new Vector3f(12.3f, 23.4f, 34.5f);
      System.out.println(p.toString());
      for (int i = 0; i < 100000; i++) {
	  t.transform(p);
	  //      System.out.println(p.toString());
	  TransMat4f r = new TransMat4f(t);
	  r.transpose();
	  r.transform(p);
      }
      System.out.println(p.toString());
  }
*/
    /*
  public static void main(String args[]) {
    float pitch, roll, yaw;
    if (args.length != 3) {
      System.err.println("usage TransMat4f  pitch_deg roll_deg yaw_deg");
      System.exit(0);
    }
    pitch = Float.parseFloat(args[0]);
    roll = Float.parseFloat(args[1]);
    yaw = Float.parseFloat(args[2]);
    // 設定した値が正しく得られるかどうかをテストする
    System.out.println("test1");
    TransMat4f t = new TransMat4f(new Euler(pitch, roll, yaw));
    Euler e =t.getEuler();
    System.out.println(e.toString());
    // 回転した点を正しくもとの位置に戻すことが出来るかどうかをテストする
    System.out.println("test2");
    Vector3f p = new Vector3f(10.0F, 20.0F, 30.0F);
    System.out.println(p.toString());
    t.transform(p);
    System.out.println(p.toString());
    TransMat4f r = new TransMat4f(t);
    r.transpose();
    r.transform(p);
    System.out.println(p.toString());
  }
    */
} // end of class TransMat4f
