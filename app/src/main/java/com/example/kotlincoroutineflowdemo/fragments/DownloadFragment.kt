package com.example.kotlincoroutineflowdemo.fragments

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kotlincoroutineflowdemo.databinding.FragmentDownloadBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import java.io.File
import java.time.Duration

class DownloadFragment : Fragment() {
    private var viewBinding: FragmentDownloadBinding? = null

    private var downloadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewBinding = FragmentDownloadBinding.inflate(inflater, container, false)
        viewBinding?.apply {
            btnStart.setOnClickListener {
                downloadJob = lifecycleScope.launch {
                    context?.apply {
                        val targetFile =
                            File(
                                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path,
                                "scene.webp"
                            )
                        try {
                            DownloadManager.download(targetFile).collect { status ->
                                when (status) {
                                    is DownloadStatus.Started -> {
                                        Toast.makeText(this, "下载开始了！", Toast.LENGTH_SHORT)
                                            .show()
                                    }

                                    is DownloadStatus.Downloading -> {
                                        viewBinding?.apply {
                                            "${
                                                String.format(
                                                    "%.2f",
                                                    status.progress
                                                )
                                            }%".also { tvProgress.text = it }
                                            progressBar.progress = status.progress.toInt()
                                        }
                                    }

                                    is DownloadStatus.Done -> {
                                        Toast.makeText(this, "下载完成了！", Toast.LENGTH_LONG)
                                            .show()
                                    }

                                    is DownloadStatus.Error -> {
                                        Toast.makeText(this, "下载失败了！", Toast.LENGTH_LONG)
                                            .show()
                                    }
                                }
                            }
                        } catch (e: CancellationException) {
                            Toast.makeText(activity, "下载取消了！", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            }

            btnCancel.setOnClickListener {
                downloadJob?.cancel()
            }
        }
        return viewBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}

val downloadRetrofit: Retrofit by lazy {
    val okhttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(30))
        .writeTimeout(Duration.ofSeconds(30))
        .build()

    Retrofit.Builder()
        .client(okhttpClient)
        .baseUrl("https://tosv.byted.org/")
        .build()
}

interface IDownloadService {
    /**
     * Download: https://tosv.byted.org/obj/gsdk-components-platform-internal/Unity/2023.6.0.0/overseas/6597549b14e530002c5af0b5-i1Ac16Hi/toutiao-i18n.apk
     */
    @Streaming
    @GET("obj/gsdk-components-platform-internal/Unity/2023.6.0.0/overseas/6597549b14e530002c5af0b5-i1Ac16Hi/toutiao-i18n.apk")
    suspend fun downloadFile(): ResponseBody
}

object DownloadManager {
    fun download(file: File): Flow<DownloadStatus> {
        val downloadService = downloadRetrofit.create(IDownloadService::class.java)
        return flow {
            emit(DownloadStatus.Started(file))
            val responseBody = downloadService.downloadFile()
            with(responseBody) {
                val totalSize = contentLength()
                var curReadSize = 0L
                val buffer = ByteArray(1024 * 8)
                byteStream().use { inputStream ->
                    file.outputStream().use { outputStream ->
                        while (true) {
                            val readSize = inputStream.read(buffer)
                            if (readSize == -1) {
                                // EOF
                                emit(DownloadStatus.Done(file))
                                break
                            } else {
                                curReadSize += readSize
                                Log.d(
                                    "GAOCHAO",
                                    "正在下载中, byte read: $curReadSize, total bytes: $totalSize"
                                )
                                emit(DownloadStatus.Downloading(curReadSize * 100.0 / totalSize))
                            }
                            outputStream.write(buffer, 0, readSize)
                        }
                    }
                }
            }
        }.catch {
            Log.d("GAOCHAO", "下载出现异常了：$it")
            file.delete()
            emit(DownloadStatus.Error(it))
        }.flowOn(Dispatchers.IO)
    }
}

sealed class DownloadStatus {
    data class Started(val file: File) : DownloadStatus()

    data class Downloading(val progress: Double) : DownloadStatus()

    data class Done(val file: File) : DownloadStatus()

    data class Error(val throwable: Throwable) : DownloadStatus()
}