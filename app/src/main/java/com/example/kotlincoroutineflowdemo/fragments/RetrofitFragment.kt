package com.example.kotlincoroutineflowdemo.fragments

import android.app.Application
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlincoroutineflowdemo.databinding.ArticleItemBinding
import com.example.kotlincoroutineflowdemo.databinding.FragmentRetrofitBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class RetrofitFragment : Fragment() {
    private var viewBinding: FragmentRetrofitBinding? = null

    private val viewModel by viewModels<ArticleViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewBinding = FragmentRetrofitBinding.inflate(inflater, container, false)

        // setup views.
        val articleListAdapter = ArticleListAdapter()
        viewBinding?.apply {
            rcvSearchResult.adapter = articleListAdapter
        }

        lifecycleScope.launch {
            viewBinding?.apply {
                etSearchKey.textWatcherFlow().collect {
                    viewModel.searchArticles(it)
                }
            }
        }
        viewModel.articles.observe(viewLifecycleOwner) {
            articleListAdapter.updateData(it)
        }

        return viewBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}

class ArticleListAdapter : RecyclerView.Adapter<ArticleListAdapter.ViewHolder>() {

    private val articles = mutableListOf<Article>()

    fun updateData(articleList: List<Article>) {
        articles.clear()
        articles.addAll(articleList)
        notifyDataSetChanged()
    }

    class ViewHolder(val articleViewBinding: ArticleItemBinding) :
        RecyclerView.ViewHolder(articleViewBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ArticleItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]
        holder.articleViewBinding.tvArticleId.text = article.id.toString()
        holder.articleViewBinding.tvArticleText.text = article.text
    }

    override fun getItemCount(): Int {
        return articles.size
    }
}

class ArticleViewModel(application: Application) : AndroidViewModel(application) {
    val articles = MutableLiveData<List<Article>>()

    fun searchArticles(key: String) {
        viewModelScope.launch {
            flow {
                val list = articleRetrofit.create(ArticleService::class.java).searchArticles(key)
                emit(list)
            }.flowOn(Dispatchers.IO).catch { e -> e.printStackTrace() }.collect {
                articles.value = it
            }
        }
    }
}

data class Article(val id: Int, val text: String)

class FakeNetworkInterceptor : Interceptor {
    private val FAKE_RESULTS = listOf(
        Article(0, "Hello, coroutines!"),
        Article(1, "My favorite feature"),
        Article(2, "Async made easy"),
        Article(3, "Coroutines by example"),
        Article(4, "Check out the Advanced Coroutines codelab next!"),
        Article(
            5,
            "Jetpack is a suite of libraries to help developers follow best practices, reduce boilerplate code, and write code that works consistently across Android versions and devices so that developers can focus on the code they care about."
        ),
        Article(
            6,
            "Jetpack Compose 1.5.0 moves to stable and brings major performance improvements including a refactoring of high-level modifiers such as `Clickable` that can improve composition time by 80%. August‘23 Compose also brings up to 70% improvement in memory allocation (especially in the graphics stack), which will reduce the memory footprint of compose on devices"
        )
    )

    private val gson = Gson()

    private fun pretendForNetDelay() = Thread.sleep(300)

    override fun intercept(chain: Interceptor.Chain): Response {
        pretendForNetDelay()
        return makeResult(chain.request())
    }

    private fun makeResult(request: Request): Response {
        val articles = mutableListOf<Article>()

        val searchKey = request.url().queryParameter("key")
        if (searchKey != null) {
            FAKE_RESULTS.filter { it.text.contains(searchKey) }.forEach { articles.add(it) }
        }

        return Response.Builder()
            .code(200)
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .message("OK")
            .body(
                ResponseBody.create(
                    MediaType.get("application/json"),
                    gson.toJson(articles)
                )
            )
            .build()
    }
}

val articleRetrofit: Retrofit by lazy {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(FakeNetworkInterceptor())
        .build()

    Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl("https://localhost/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

interface ArticleService {
    @GET("articles")
    suspend fun searchArticles(@Query("key") key: String): List<Article>
}

private fun TextView.textWatcherFlow(): Flow<String> = callbackFlow {
    val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            trySend(s.toString())
        }
    }

    addTextChangedListener(textWatcher)
    awaitClose { removeTextChangedListener(textWatcher) } // 当 flow 关闭的时候，移除监听
}
