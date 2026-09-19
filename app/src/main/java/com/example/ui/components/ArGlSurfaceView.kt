package com.example.ui.components

import android.app.Activity
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ar.ArCoreScanEngine

@Composable
fun ArGlSurfaceView(
  scanEngine: ArCoreScanEngine,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val activity = context as? Activity

  var glView: GLSurfaceView? = null

  DisposableEffect(lifecycleOwner, scanEngine) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          activity?.let { scanEngine.resume(it) }
          glView?.onResume()
        }
        Lifecycle.Event.ON_PAUSE -> {
          glView?.onPause()
          scanEngine.pause()
        }
        Lifecycle.Event.ON_DESTROY -> {
          scanEngine.destroy()
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      glView?.onPause()
      scanEngine.pause()
    }
  }

  AndroidView(
    factory = { ctx ->
      GLSurfaceView(ctx).apply {
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2)
        setRenderer(scanEngine)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glView = this
      }
    },
    modifier = modifier
  )
}
