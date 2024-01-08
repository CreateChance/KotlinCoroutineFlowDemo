package com.example.kotlincoroutineflowdemo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.kotlincoroutineflowdemo.databinding.FragmentSharedFlowBinding
import com.example.kotlincoroutineflowdemo.databinding.FragmentTextBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class SharedFlowFragment : Fragment() {
    private var viewBinding: FragmentSharedFlowBinding? = null

    private val viewModel by viewModels<TimestampViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewBinding = FragmentSharedFlowBinding.inflate(inflater, container, false)
        viewBinding?.apply {
            btnStart.setOnClickListener {
                viewModel.start()
            }
            btnStop.setOnClickListener {
                viewModel.stop()
            }
        }
        return viewBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}

class TimestampViewModel : ViewModel() {

    private var job: Job? = null

    fun start() {
        job = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                timestampFlow.emit(System.currentTimeMillis())
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}

class TextFragment : Fragment() {

    private var viewBinding: FragmentTextBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentTextBinding.inflate(inflater, container, false)
        viewBinding?.apply {
            lifecycleScope.launch {
                timestampFlow.collect {
                    tvTimestamp.text = it.toString()
                }
            }
        }
        return viewBinding?.root
    }
}

val timestampFlow = MutableSharedFlow<Long>()
