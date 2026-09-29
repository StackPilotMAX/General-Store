package com.stackpilotmax.rameshvegetableshop

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap as toComposeImageBitmap

internal fun Bitmap.asImageBitmap(): ImageBitmap = this.toComposeImageBitmap()
