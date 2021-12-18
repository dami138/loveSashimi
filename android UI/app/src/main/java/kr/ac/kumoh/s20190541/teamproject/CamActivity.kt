/**
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.ac.kumoh.s20190541.teamproject

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.torchvision.TensorImageUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min


class CamActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        const val TAG = "TFLite - ODT"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        private const val MAX_FONT_SIZE = 70F
    }

    private lateinit var captureImageFab: Button
    private lateinit var inputImageView: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var currentPhotoPath: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cam)

        captureImageFab = findViewById(R.id.captureImageFab)
        inputImageView = findViewById(R.id.imageView)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)

        captureImageFab.setOnClickListener(this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE &&
            resultCode == Activity.RESULT_OK
        ) {
            setViewAndDetect(getCapturedImage())
        }
    }

    /**
     * onClick(v: View?)
     *      Detect touches on the UI components
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.captureImageFab -> {
                try {
                    dispatchTakePictureIntent()
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
        }
    }

//모델 절대주소를 얻는 함수
    fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            return file.absolutePath
        }
    }
    
    
// 모델 불러와 이미지 입력
    private fun runObjectDetection(bitmap: Bitmap) {
        val mModule = LiteModuleLoader.load(assetFilePath(getApplicationContext(), "test.ptl"));
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        val outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        val outputTensor = outputTuple[0].toTensor()
        val outputs = outputTensor.getDataAsFloatArray();


        drawDetectionResult(bitmap, outputs)
    }

//불러온 이미지를 이미지 뷰에 출력 후 runObjectDetection호출
    private fun setViewAndDetect(bitmap: Bitmap) {
        // Display capture image

        inputImageView.setImageBitmap(bitmap)
        tvPlaceholder.visibility = View.INVISIBLE

        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        lifecycleScope.launch(Dispatchers.Default) { runObjectDetection(bitmap) }
    }

//카메라로 찍은 이미지를 불러온 후 비트맵으로 변경 후 이미지 뷰에 맞게 스케일 조절 
//스케일 조절시 이상하게 이미지가 회전됨
//ExifInterface 함수를 사용해 이미지 파일에 접근하여 이미지 파일의 정보를 불러와 원본 이미지에 맞게 회전

    private fun getCapturedImage(): Bitmap {
        // Get the dimensions of the View
        val targetW: Int = inputImageView.width
        val targetH: Int = inputImageView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = max(1, min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inMutable = true
        }
        val exifInterface = ExifInterface(currentPhotoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_ROTATE_90
        )

        val bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 270f)
            }
            else -> {
                bitmap
            }
        }
    }

//drawable에 있는 이미지의 주소를 불러오기위한 함수
    open fun getURLForResource(resourceId: Int): String {
        //use BuildConfig.APPLICATION_ID instead of R.class.getPackage().getName() if both are not same
        return Uri.parse("android.resource://" + R::class.java.getPackage().name + "/" + resourceId).toString()
    }


    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

//사진을 찍을 시 임시파일에 저장해 주는 함수 같음

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)



        return File(
            storageDir, /* directory */
            "JPEG_${timeStamp}_"+".jpg"
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }

    }


    //카메라 모듈 호출 함수 같음
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (e: IOException) {
                    Log.e(TAG, e.message.toString())
                    null
                }


                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "kr.ac.kumoh.s20190541.teamproject",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

//이미지에 바운딩 박스와 라벨을 그려주는 함수
//바운딩 박스가 2개 씩 그려지는 것 같음
//for문이나 return된 이미지를 다른 곳에서 사용하는 듯 

//모델 아웃풋이 FloatArray로 출력되는데 0:x, 1:y, 2:w, 3:h, 4:아마 객체 중 가장 큰 점수, 5~9:객 객체의 점수 
//아마 x,y는 객체의 중앙 픽셀 그래서 전체 픽셀 수 만큼 for문을 
    private fun drawDetectionResult(
        bitmap: Bitmap,
        outputs: FloatArray
    ): Bitmap {


        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        canvas.drawColor(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT



        val imgScaleX = bitmap.getWidth().toFloat() / PrePostProcessor.mInputWidth;
        val imgScaleY = bitmap.getHeight().toFloat() / PrePostProcessor.mInputHeight;
        val ivScaleX = inputImageView.width.toFloat() / bitmap.getWidth();
        val ivScaleY = inputImageView.height.toFloat() / bitmap.getHeight();


        val results = PrePostProcessor.outputsToNMSPredictions(outputs, imgScaleX, imgScaleY, ivScaleX, ivScaleY,0.toFloat(),0.toFloat());

        //두번째 페이지로 띄울 회 레이블 담을 변수
        val labels =  ArrayList<String>()

        for(i in 0 until(results.size)){
                pen.color = Color.WHITE
                pen.strokeWidth = 7F
                pen.style = Paint.Style.STROKE


                canvas.drawRoundRect(results[i].rect, 80F, 80F, pen)


                val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
                pen.style = Paint.Style.FILL_AND_STROKE
                pen.color = Color.WHITE
                pen.strokeWidth = 2F
                val label = arrayOf("우럭", "광어", "참치", "연어", "밀치")

                if (label[results[i].classIndex] !in labels){
                    labels.add(label[results[i].classIndex])
                }

                pen.textSize = MAX_FONT_SIZE
                pen.getTextBounds(label[results[i].classIndex], 0, label[results[i].classIndex].length, tagSize)
                val fontSize: Float = pen.textSize * results[i].rect.width() / tagSize.width()

                // adjust the font size so texts are inside the bounding box
                if (fontSize < pen.textSize) pen.textSize = fontSize

                var margin = (results[i].rect.width() - tagSize.width()) / 2.0F
                if (margin < 0F) margin = 0F
                canvas.drawText(
                    label[results[i].classIndex], results[i].rect.left + margin - 6,
                    results[i].rect.top + tagSize.height().times(1F) + 13, pen
                )
            }

        inputImageView.setImageBitmap(outputBitmap)
        tvPlaceholder.visibility = View.INVISIBLE


        // firebase DB연결
        var db = FirebaseFirestore.getInstance()

        // firebase 데이터 불러옴
        val rawFishDB = db.collection("RawFish")

        var rawFish = HashMap<String,Any>()

        //변수에 있는 회들 보내기
        for (labelName in labels) {
            //이름 같은 회 있는지 검색
            rawFishDB.whereEqualTo("Name", labelName)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        rawFish = document.data as HashMap<String, Any>
                    }
                    if (rawFish.size != 0) {
                        // 두번째 화면
                        val intent = Intent(this, MainActivity2::class.java)
                        //intent.putExtra("mode", "search")

                        // 선택된 회들 intent로 두번째 화면에 보내기
                        intent.putExtra("rawFish", rawFish)


                        //비트맵 이미지 보내기
                        var stream = ByteArrayOutputStream()
                        var resize = Bitmap.createScaledBitmap(outputBitmap, 500, 500, false)
                        resize.compress(Bitmap.CompressFormat.JPEG,100,stream)


                        intent.putExtra("output",stream.toByteArray())

                        startActivity(intent)

                    } else {
                        Toast.makeText(this, "No Match found", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(ContentValues.TAG, "Error getting documents: ", exception)
                }


        }

        return outputBitmap

    }
}
/**
 * DetectionResult
 *      A class to store the visualization info of a detected object.
 */

