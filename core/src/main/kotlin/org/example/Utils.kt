package org.example

import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array

fun ULong.toBitList(width: Int = 41): D1Array<Int> {
  return this.toLong()
    .toString(2)
    .padStart(width, '0')
    .map { (it - '0') }.toNDArray()
}

data class Quintuple<out A, out B, out C, out D, out E>(
  public val first: A,
  public val second: B,
  public val third: C,
  public val fourth: D,
  public val fifth: D,
)