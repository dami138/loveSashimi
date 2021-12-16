package kr.ac.kumoh.s20190541.teamproject

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kr.ac.kumoh.s20190541.teamproject.databinding.ActivityMainBinding
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    // firebase DB연결
    var db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityMainBinding
    private lateinit var photoFile: File
    private var REQUEST_CODE_FOR_IMAGE_CAPTURE = 123
    private lateinit var searchView: SearchView
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.cameraBtn.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                //val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val dir = externalCacheDir
                val file = File.createTempFile("photo_", ".jpg", dir)
                val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                startActivityForResult(intent, REQUEST_CODE_FOR_IMAGE_CAPTURE)
                photoFile = file
            }
        }

        //두번째 화면으로 값을 전달하기 위한 해쉬맵
        var rawFish = HashMap<String,Any>()

        //오늘은 몇월인지 계산
        val current = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("M")
        val season = current.format(formatter)
        //Log.d(TAG, "오늘은"+season)

        // firebase 데이터 불러옴
        val rawFishDB = db.collection("RawFish")

        // 시즌 레시피에 이번 달의 제철 회가 뜸
        rawFishDB.whereEqualTo("Season", season.toInt())
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    rawFish = document.data as HashMap<String, Any>
                    Log.d(TAG, "제철 회 추천기능"+rawFish)

                    //사진 변경
                    Glide.with(this).load("${document.getData().get("Image")}")
                        .into(rec_btn)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Error getting documents: ", exception)
            }


        rec_btn.setOnClickListener {

            val intent = Intent(this, MainActivity2::class.java)
            //intent.putExtra("mode", "recommend")

            // 선택된 회들 intent로 두번째 화면에 보내기
            intent.putExtra("rawFish", rawFish)

            startActivity(intent)
        }

        // 검색창 기능 구현
        searchView = findViewById(R.id.search)

        var check = 0
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                //검색 키워드(query)와 똑같은 Name을 가진 회가 있는지 DB에서 탐색
                rawFishDB.whereEqualTo("Name", query)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            rawFish = document.data as HashMap<String, Any>
                            Log.d(TAG, "검색기능"+rawFish)
                            check = 1
                        }
                        if (check == 1 && rawFish.size != 0) {
                            // 두번째 화면
                            val intent = Intent(this@MainActivity, MainActivity2::class.java)
                            //intent.putExtra("mode", "search")

                            // 선택된 회들 intent로 두번째 화면에 보내기
                            intent.putExtra("rawFish", rawFish)
                            Log.d(TAG, "체크")
                            startActivity(intent)
                            check = 0
                        }
                        else {
                            Log.d(ContentValues.TAG, "Error getting documents: ")
                            Toast.makeText(this@MainActivity, "해당 회 없음", Toast.LENGTH_LONG).show()
                        }
                    }
                    //.addOnFailureListener { exception ->
                    //    Log.d(ContentValues.TAG, "Error getting documents: ", exception)
                    //    Toast.makeText(this@MainActivity, "No Match found", Toast.LENGTH_LONG).show()
                    //}
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })



        diary_btn1.setOnClickListener {

        }

        diary_btn2.setOnClickListener {

        }

        diary_btn3.setOnClickListener {

        }

        diary_btn4.setOnClickListener {

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_FOR_IMAGE_CAPTURE -> {
                if (resultCode == RESULT_OK) {

                } else {
                    Toast.makeText(this, "취소 되었습니다", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}