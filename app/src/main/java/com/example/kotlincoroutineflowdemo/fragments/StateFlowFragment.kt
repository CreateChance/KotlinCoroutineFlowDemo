package com.example.kotlincoroutineflowdemo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.example.kotlincoroutineflowdemo.databinding.FragmentStateFlowBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class StateFlowFragment : Fragment() {
    private var viewBinding: FragmentStateFlowBinding? = null

    private val viewModel by viewModels<NumberViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewBinding = FragmentStateFlowBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            viewBinding?.apply {
                btnAdd.setOnClickListener {
                    viewModel.add()
                }
                btnMinus.setOnClickListener {
                    viewModel.minus()
                }
                viewModel.number.collect {
                    tvNumber.text = it.toString()
                }
            }
        }

        return viewBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}

class NumberViewModel : ViewModel() {

    val number = MutableStateFlow(0)

    fun add() {
        number.value++
    }

    fun minus() {
        number.value--
    }
}
