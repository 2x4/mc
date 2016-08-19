//			    Vector3f.java
//
//		  Copyright (C) 1999 Takashi Yukawa
//
//		    This java source file conforms
//		GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawa@jc.akeihou-u.ac.jp>
// Created: Jan.17, 1999
package vecmath;

//import java.text.*;

public class Vector3f {
  public float x;
  public float y;
  public float z;

  public Vector3f() {
    x = 0.0F;
    y = 0.0F;
    z = 0.0F;
  }

  public Vector3f(float x, float y, float z) {
    this.set(x, y, z);
  }

  public Vector3f(Vector3f v) {
    this.set(v.x, v.y, v.z);
  }

  public final void set(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  // sets the value of this vector3f to v.
  public final void set(Vector3f v) {
    this.set(v.x, v.y, v.z);
  }

  // sets the value of this vector3f to this = this + v.
  public final void add(Vector3f v) {
    this.x += v.x;
    this.y += v.y;
    this.z += v.z;
  }

  // sets the value of this vector3f to this = this - v.
  public final void sub(Vector3f v) {
    this.x -= v.x;
    this.y -= v.y;
    this.z -= v.z;
  }
/*
  public final String toString() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);
    return String.valueOf(nf.format(x)) + " " + 
      String.valueOf(nf.format(y)) + " " + 
      String.valueOf(nf.format(z));
  }
*/
}
