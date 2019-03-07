package com.kyujin.meeco

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class Pair<P1, P2> (
    val l: P1,
    val r: P2
): Serializable