package kr.ac.kumoh.s20190541.teamproject

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        var textView = findViewById(R.id.description) as TextView
        textView.movementMethod = ScrollingMovementMethod.getInstance()

        var nameView = findViewById(R.id.name) as TextView
        var monthView = findViewById(R.id.month) as TextView

        // intent 데이터 받아오기

        var rawFish = HashMap<String,Any>()
        if (intent.hasExtra("rawFish")) {
            // "rawFish"라는 이름의 key에 저장된 값이 있다면 rawFish라는 변수에 이를 저장
            Log.d(TAG, "두번째 화면"+ intent.getSerializableExtra("rawFish"))
            rawFish = intent.getSerializableExtra("rawFish") as HashMap<String, Any>

        }

        //todo
        // Efficacy(효능) Cooking(요리) HowToEat(먹는방법) 추가하기

        //화면에 정보 띄우기
        nameView.text = rawFish.get("Name") as CharSequence?

        Glide.with(this).load(rawFish.get("Image").toString())
            .into(image2)

        monthView.text = "제철: " + rawFish.get("Season").toString() + "월"

        textView.text = rawFish.get("Description") as CharSequence?






    }
}