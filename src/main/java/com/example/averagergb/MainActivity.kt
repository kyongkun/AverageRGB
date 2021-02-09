package com.example.averagergb

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.averagergb.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias ColourListener = (R: Double, G: Double, B: Double) -> Unit
class NameViewModel : ViewModel() {

    // Create a LiveData with a String
    val currentName: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    val width: Int = Resources.getSystem().displayMetrics.widthPixels
    val height: Int = Resources.getSystem().displayMetrics.heightPixels
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        Log.d(TAG, "x: "+ binding.imageView2.x.toString() + "y: " + binding.imageView2.y.toString())
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }



    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d(TAG, "x: "+ binding.imageView2.x.toString() + "y: " + binding.imageView2.y.toString())
        binding.imageView2.x = ((253.0-15.0)/720.0*width.toDouble()).toFloat()
        binding.imageView2.y = ((650.0-15.0)/1280.0*height.toDouble()).toFloat()
        binding.RGBvalue.text = rgbvalue
        binding.RGBvalue.bringToFront()
        Log.d(TAG, "x: "+ binding.imageView2.x.toString() + "y: " + binding.imageView2.y.toString())
        return super.onTouchEvent(event)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

       // val rotation = binding.viewFinder.display.rotation
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                    //.setTargetRotation(rotation)
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

            val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(720, 1280))

               //     .setTargetRotation()
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ColourAnalyzer { R, G, B ->
                            Log.d(TAG, "R: $R, G:, $G, B: $B")
                        })
                    }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private val TAG = "ClassName"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private var rgbvalue = ""
        private var xc = 0
        private var yc = 0
    }

    class ColourAnalyzer(private val listener: ColourListener) : ImageAnalysis.Analyzer {

        lateinit var result: String
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {
            val RGB = getRGBfromImgproxy2(image)
            listener(RGB.first, RGB.second, RGB.third)
            result = "R: ${RGB.first.toInt()} G: ${RGB.second.toInt()} G: ${RGB.third.toInt()}"
            rgbvalue = result

            image.close()
        }

        fun getRGBfromImgproxy2(image: ImageProxy): Triple<Double, Double, Double> {
            val planes = image.planes
            //     val height = 253
            //     val width = 650
            //Y
            val yArr = planes[0].buffer
            val yArrByteArray = yArr.toByteArray()
            val yPixelStride = planes[0].pixelStride
            val yRowStride = planes[0].rowStride
            //U
            val uArr = planes[1].buffer
            val uArrByteArray = uArr.toByteArray()
            val uPixelStride = planes[1].pixelStride
            val uRowStride = planes[1].rowStride

            //V
            val vArr = planes[2].buffer
            val vArrByteArray = vArr.toByteArray()
            val vPixelStride = planes[2].pixelStride
            val vRowStride = planes[2].rowStride
            var y = 0.0
            var u = 0.0
            var v = 0.0
            var r = 0.0
            var g = 0.0
            var b = 0.0
            var areacounter = 0
            //    for (i in xc..xc) {
            //       for (j in yc..yc) {
            var ypixels:Array<Int> = emptyArray()
            var upixels:Array<Int> = emptyArray()
            var vpixels:Array<Int> = emptyArray()
            for (i in 238..267) {
                for (j in 635..664) {
                    //for (i in 1..20) {
                    //   for (j in 1..20) {
                    ypixels += (yArrByteArray[(i * yRowStride + j * yPixelStride)].toInt() and 255)
                    upixels += ((uArrByteArray[(i/4 * uRowStride + j/4 * uPixelStride)].toInt() and 255) - 128)
                    vpixels += ((vArrByteArray[(i/4 * vRowStride + j/4 * vPixelStride)].toInt() and 255) - 128)

                }
            }
            y = ypixels.average()
            u = upixels.average()
            v = vpixels.average()
            r = (y + (1.370705 * v))
            g = (y - (0.698001 * v) - (0.337633 * u))
            b = (y + (1.732446 * u))
            return Triple(r, g, b)
        }

    }

}