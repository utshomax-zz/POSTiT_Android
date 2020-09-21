package com.madvert.postit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.ybq.android.spinkit.style.Wave
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        loginprocess.visibility=View.INVISIBLE;
        loginBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                lifecycleScope.launch {
                    var username = userbox.text.toString()
                    var password = passwordbox.text.toString()
                    if (username.isEmpty() || password.isEmpty() || (username.length < 6) || (password.length < 6)) {
                        Toast.makeText(
                            this@login,
                            "Check Your Username and Password",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        try {
                            loginprocess.setIndeterminateDrawable(Wave());
                            loginprocess.visibility = View.VISIBLE;
                            val response = isUser(username, password);
                            if (response["status"].toString() == "1") {
                                Log.d("I",response.toString());
                                try {
                                    var userdata: SharedPreferences =
                                        getSharedPreferences("USERDATA", Context.MODE_PRIVATE);
                                    val editor: Editor = userdata.edit()
                                    editor.putString("name", response["name"].toString());
                                    editor.putString("post_n", response["postname"].toString());
                                    editor.putString("post_no", response["postcode"].toString());
                                    editor.putString("city", response["city"].toString());
                                    editor.apply()
                                    editor.commit()
                                }
                                catch (e:java.lang.Exception){
                                    Toast.makeText(this@login, "Server Response Problem", Toast.LENGTH_SHORT)
                                        .show()
                                }

                                Toast.makeText(this@login, "Welcome " + username, Toast.LENGTH_LONG)
                                    .show()

                                val intent = Intent(this@login, MainActivity::class.java)
                                intent.putExtra("name", username)
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    this@login,
                                    "Check Your Username and Password",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loginprocess.visibility = View.INVISIBLE;
                            }
                        } catch (e: Exception) {
                            loginprocess.visibility = View.INVISIBLE;
                            Toast.makeText(this@login, "Somthing Wrong!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }


            }
        })
    }

    private suspend fun isUser(userName: String, password: String): JSONObject {
        val result = withContext(Dispatchers.IO) {
            var reqParam =
                URLEncoder.encode("uid", "UTF-8") + "=" + URLEncoder.encode(userName, "UTF-8")
            reqParam += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(
                password,
                "UTF-8"
            )
            val mURL = URL("http://192.168.0.102:3000/api/isUser")

            with(mURL.openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "POST"

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
                    return@withContext obj;
                }
            }
        }
        return result;
    }

}