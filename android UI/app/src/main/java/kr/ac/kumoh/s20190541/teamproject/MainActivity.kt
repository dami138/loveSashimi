package kr.ac.kumoh.s20190541.teamproject

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import kr.ac.kumoh.s20190541.teamproject.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var photoFile: File
    private var REQUEST_CODE_FOR_IMAGE_CAPTURE = 123
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

        rec_btn.setOnClickListener {
            // 두번째 화면 테스트
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }

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