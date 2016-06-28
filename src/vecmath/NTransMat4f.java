package vecmath;

import java.text.NumberFormat;

/**
 * 回転行列をあらわす4x4行列
 */
public class NTransMat4f {
  float m00, m01, m02, m03;
  float m10, m11, m12, m13;
  float m20, m21, m22, m23;
  float m30, m31, m32, m33;

  public NTransMat4f() {
      m00 = 1.0f;       m01 = 0.0f;       m02 = 0.0f;       m03 = 0.0f;
      m10 = 0.0f;       m11 = 1.0f;       m12 = 0.0f;       m13 = 0.0f;
      m20 = 0.0f;       m21 = 0.0f;       m22 = 1.0f;       m23 = 0.0f;
      m30 = 0.0f;       m31 = 0.0f;       m32 = 0.0f;       m33 = 1.0f;
  }

  public NTransMat4f(NTransMat4f t) {
    m00 = t.m00;    m01 = t.m01;    m02 = t.m02;    m03 = t.m03;
    m10 = t.m10;    m11 = t.m11;    m12 = t.m12;    m13 = t.m13;
    m20 = t.m20;    m21 = t.m21;    m22 = t.m22;    m23 = t.m23;
    m30 = t.m30;    m31 = t.m31;    m32 = t.m32;    m33 = t.m33;
  }

  public NTransMat4f(Euler rot) {
    this.setRotation(rot);
  }

  /**
   * この行列とベクタvを乗算して，その結果をベクタvに保存する．
   */
  public native void transform(Vector3f v);

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
   *
   */
    public native void setRotation(float pitch, float roll, float yaw);

  public native void mul(TransMat4f t);

  public native Euler getEuler();

    public String toString() {
	NumberFormat nf = NumberFormat.getInstance();
	nf.setMaximumFractionDigits(4);
	nf.setMinimumFractionDigits(4);
	return String.valueOf(nf.format(m00)) + ", " +
	    String.valueOf(nf.format(m01)) + ", " +
	    String.valueOf(nf.format(m02)) + ", " +
	    String.valueOf(nf.format(m03)) +
	    System.getProperty("line.separator") +
	    String.valueOf(nf.format(m10)) + ", " +
	    String.valueOf(nf.format(m11)) + ", " +
	    String.valueOf(nf.format(m12)) + ", " +
	    String.valueOf(nf.format(m13)) +
	    System.getProperty("line.separator") +
	    String.valueOf(nf.format(m20)) + ", " +
	    String.valueOf(nf.format(m21)) + ", " +
	    String.valueOf(nf.format(m22)) + ", " +
	    String.valueOf(nf.format(m23)) +
	    System.getProperty("line.separator") +
	    String.valueOf(nf.format(m30)) + ", " +
	    String.valueOf(nf.format(m31)) + ", " +
	    String.valueOf(nf.format(m32)) + ", " +
	    String.valueOf(nf.format(m33));
    }

    static {
      System.loadLibrary("NTransMat4fImp");
    }

  public static void main(String args[]) {
      float DEG2RAD = (float)(Math.PI / 180.0);
      float pitch = 30.0f;
      float roll = 45.0f;
      float yaw = 60.0f;
      //      NTransMat4f t = new NTransMat4f(new Euler(pitch, roll, yaw));
      NTransMat4f t = new NTransMat4f();
      t.setRotation(pitch*DEG2RAD, roll*DEG2RAD, yaw*DEG2RAD);
      //      System.out.println(t.toString());
      Euler e = t.getEuler();
      //      System.out.println(e.toString());
      Vector3f p = new Vector3f(12.3f, 23.4f, 34.5f);
      System.out.println(p.toString());
      for (int i = 0; i < 100000; i++) {
	  t.transform(p);
	  //      System.out.println(p.toString());
	  NTransMat4f r = new NTransMat4f(t);
	  r.transpose();
	  r.transform(p);
      }
      System.out.println(p.toString());
  }
} // end of class TransMat4f
