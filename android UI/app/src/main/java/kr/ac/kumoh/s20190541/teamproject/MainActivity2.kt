package kr.ac.kumoh.s20190541.teamproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        var textView = findViewById(R.id.description) as TextView

        textView.movementMethod = ScrollingMovementMethod.getInstance()

    }
}