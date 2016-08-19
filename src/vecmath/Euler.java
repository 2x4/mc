//
// Euler.java
//
// Copyright (C) 1998-2016 Takashi Yukawa
// This source is licenced under the MIT license
// https://github.com/2x4/mc/blob/master/LICENSE

package vecmath;

// Objectの回転を表現するクラス 内部では回転の値をradianで保持する．
public class Euler {
  public static final float RAD2DEG = (float)(180.0 / Math.PI);
  public static final float DEG2RAD = (float)(Math.PI / 180.0);
  float pitch, roll, yaw;

  public Euler() {
    this(0.0F ,0.0F, 0.0F); 
  }

  public Euler(Euler e) {
    this.set(e);
  }

  //public Euler(Vector3f v) {
  //  this.setDeg(v.y, v.x, v.z);
  //}
  
  public Euler(float pitch, float roll, float yaw) {
    setPitchDeg(pitch);
    setRollDeg(roll);
    setYawDeg(yaw);
  }

  public final void set(Euler e) {
    pitch = e.pitch;
    roll = e.roll;
    yaw = e.yaw;
  }

  //public final void sub(Euler e) {
  //  pitch -= e.pitch;
  //  roll -= e.roll;
  //  yaw -= e.yaw;
  //}

  //public final void add(Euler e) {
  //  pitch += e.pitch;
  //  roll += e.roll;
  //  yaw += e.yaw;
  //}

  // 値をradianで設定
  public final void setPitchRad(float pitch) { this.pitch = pitch; }
  public final void setRollRad(float roll)   { this.roll  = roll;  }
  public final void setYawRad(float yaw)     { this.yaw   = yaw;   }
  //public final void setRad(float pitch, float roll, float yaw) {
  //  setPitchRad(pitch);
  //  setRollRad(roll);
  //  setYawRad(yaw);
  //}

  // 値をdegreeで設定
  public final void setPitchDeg(float pitch) { this.pitch = pitch * DEG2RAD; }
  public final void setRollDeg(float roll)   { this.roll  = roll * DEG2RAD;  }
  public final void setYawDeg(float yaw)     { this.yaw   = yaw * DEG2RAD;   }
  //public final void setDeg(float pitch, float roll, float yaw) {
  //  setPitchDeg(pitch);
  //  setRollDeg(roll);
  //  setYawDeg(yaw);
  //}
  //
  public final float getPitchRad() { return pitch; }
  public final float getRollRad()  { return roll;  }
  public final float getYawRad()   { return yaw;   }
  //
  public final float getPitchDeg() { return pitch * RAD2DEG; }
  public final float getRollDeg()  { return roll  * RAD2DEG; }
  public final float getYawDeg()   { return yaw   * RAD2DEG; }

  //@Override
  //public final String toString() {
  //  NumberFormat nf = NumberFormat.getInstance();
  //  nf.setMaximumFractionDigits(4);
  //  nf.setMinimumFractionDigits(4);
  //  return "Pitch = " + String.valueOf(nf.format(getPitchDeg())) +
  //         ", Roll = " + String.valueOf(nf.format(getRollDeg())) +
  //         ", Yaw = " + String.valueOf(nf.format(getYawDeg()));
  //}
}
