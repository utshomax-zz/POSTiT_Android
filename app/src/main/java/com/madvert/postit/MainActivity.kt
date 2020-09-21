package com.madvert.postit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Camera
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import com.github.ybq.android.spinkit.SpinKitView
import com.github.ybq.android.spinkit.sprite.Sprite
import com.github.ybq.android.spinkit.style.DoubleBounce
import com.github.ybq.android.spinkit.style.RotatingPlane
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val  requestCamaraPermission=1001
    private lateinit var cameraSource: CameraSource
    private lateinit var detector: BarcodeDetector
    private var flashmode: Boolean = false
    private var OnCamera: Boolean= true
    private var camera: Camera? = null
    var name : String= ""
    var postname : String= ""
    var city : String= ""
    var postcode : String= ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val userdata = getSharedPreferences("USERDATA",Context.MODE_PRIVATE)
        name= userdata.getString("name","Default").toString()
        postname= userdata.getString("post_n","Default").toString()
        city= userdata.getString("city","Default").toString()
        postcode= userdata.getString("post_no","Default").toString()

        postitidbox.setText(name);
        postcodebox.setText(postcode)
        postnamebox.setText(postname)

        flashbtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                flashOnButton()
            }
        })
        spin_kit.setIndeterminateDrawable(DoubleBounce())
        btn_enable_disable.setOnClickListener(object : View.OnClickListener{
            @SuppressLint("MissingPermission")
            override fun onClick(v: View?) {
                if(OnCamera){
                    cameraSource.stop()
                }
                else{
                    this@MainActivity.recreate()
                }
                OnCamera=!OnCamera;
            }
        })
        if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            askForCamaraPermisson()
        } else {
            setupControlls()
        }
        }


    private fun setupControlls() {
            detector = BarcodeDetector.Builder(this@MainActivity)
                    .setBarcodeFormats(Barcode.CODE_128)
                    .build()
            cameraSource = CameraSource.Builder(this@MainActivity, detector)
                    .setAutoFocusEnabled(true)

                    .build()
            camaraSurfaceView.holder.addCallback(surgaceCallBack)
            detector.setProcessor(processor)



        }

        private fun askForCamaraPermisson() {
            ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(android.Manifest.permission.CAMERA),
                    requestCamaraPermission
            )
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == requestCamaraPermission && grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupControlls()
                } else {
                    Toast.makeText(applicationContext, "permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private val surgaceCallBack = object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                try {
                    cameraSource.start(surfaceHolder)
                } catch (excption: Exception) {
                    Toast.makeText(applicationContext, "Something Wrong", Toast.LENGTH_SHORT).show()
                }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraSource.stop()
            }
        }


    //RESULT PROCESSOR

        private val processor = object : Detector.Processor<com.google.android.gms.vision.barcode.Barcode> {
            override fun release() {

            }

            override fun receiveDetections(detections: Detector.Detections<com.google.android.gms.vision.barcode.Barcode>?) {
                if (detections != null && detections.detectedItems.isNotEmpty()) {
                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                        info.setText("Scaning..")
                        spin_kit.setColor(Color.GREEN)
                    })

                    var qrCodes: SparseArray<com.google.android.gms.vision.barcode.Barcode> =
                        detections.detectedItems

                    val code = qrCodes.valueAt(0)
                    val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                    val currentDate = sdf.format(Date())
                    Log.d("MSG", code.rawValue)
                    val id = code.rawValue.toString()
                    beep()
                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                        info.setText("Updating..")
                        spin_kit.setColor(Color.YELLOW)
                    })
                    try {
                        val res = sendPostRequest(
                            id,
                            city,
                            postname,
                            postcode,
                            currentDate.toString()
                        );
                        try {
                            if (res["Status"].toString() == "0") {
                                this@MainActivity.runOnUiThread(Runnable {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Please , Rescan The Barcode!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    spin_kit.setColor(Color.GREEN)
                                    info.setText("Scaning..")
                                })
                            }
                            if (res["Status"].toString() == "2") {
                                this@MainActivity.runOnUiThread(Runnable {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Allready Updated!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    spin_kit.setColor(Color.GREEN)
                                    info.setText("Scaning..")
                                })
                            }
                        } catch (e: java.lang.Exception) {
                            try {
                                if (res["b_id"] != null) {
                                    Log.i("S", "SUCCESS");
                                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                                        spin_kit.setColor(Color.GREEN)
                                        info.setText("DONE")
                                    })
                                }
                            }
                            catch (e: java.lang.Exception){
                                this@MainActivity.runOnUiThread(Runnable {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "NOT A VALID BARCODE",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    spin_kit.setColor(Color.RED)
                                    info.setText("INVALID..")
                                })
                            }
                        }
                    } catch (e: Exception) {
                        this@MainActivity.runOnUiThread(Runnable {
                            Toast.makeText(
                                this@MainActivity,
                                "Something Wrong!",
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                    }

                    //stopScan()

                } else {

                }
            }
        }


    //FLASH BUTTON


    private fun flashOnButton() {
        camera = getCamera(cameraSource)

        if (camera != null) {
            try {
                val param = camera!!.parameters

                param.setFlashMode(if (!flashmode) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF)
                camera?.parameters = param
                flashmode = !flashmode
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCamera(cameraSource: CameraSource): Camera? {
        val declaredFields = CameraSource::class.java.declaredFields

        for (field in declaredFields) {
            if (field.type === Camera::class.java) {
                field.isAccessible = true

                try {
                    val camera = field.get(cameraSource) as Camera
                    return if (camera != null) {
                        camera
                    } else null
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }

                break
            }
        }

        return null
    }


    //UTILITYS

    private fun beep(){
          val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
          toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_SLS, 25)
    }
    //Processbar


    //Requests to POSTiT SERVER



    fun sendPostRequest(id:String, city:String, postname:String,postcode:String,dateTime:String): JSONObject {

        var reqParam = URLEncoder.encode("b_id", "UTF-8") + "=" + URLEncoder.encode(id, "UTF-8")
        reqParam += "&" + URLEncoder.encode("city", "UTF-8") + "=" + URLEncoder.encode(city, "UTF-8")
        reqParam += "&" + URLEncoder.encode("post_n", "UTF-8") + "=" + URLEncoder.encode(postname, "UTF-8")
        reqParam += "&" + URLEncoder.encode("post_no", "UTF-8") + "=" + URLEncoder.encode(postcode, "UTF-8")
        reqParam += "&" + URLEncoder.encode("dateTime", "UTF-8") + "=" + URLEncoder.encode(dateTime, "UTF-8")
        val mURL = URL("http://192.168.0.102:3000/api/upLoc")

        with(mURL.openConnection() as HttpURLConnection) {
            // optional default is GET
            requestMethod = "PUT"

            val wr = OutputStreamWriter(getOutputStream());
            wr.write(reqParam);
            wr.flush();
            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                val obj = JSONObject(response.toString())
                return obj;
            }
        }
    }



}