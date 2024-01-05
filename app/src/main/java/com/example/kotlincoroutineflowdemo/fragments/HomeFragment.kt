package com.example.kotlincoroutineflowdemo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kotlincoroutineflowdemo.R
import com.example.kotlincoroutineflowdemo.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var viewBinding: FragmentHomeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        viewBinding = FragmentHomeBinding.inflate(inflater, container, false)

        setupView()

        return viewBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }

    private fun setupView() {
        viewBinding?.let {
            it.btnFlowNDownload.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_downloadFragment)
            }
            it.btnFlowNRoom.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_roomFragment)
            }
            it.btnFlowNRetrofit.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_retrofitFragment)
            }
            it.btnStateFow.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_stateFlowFragment)
            }
            it.btnSharedFow.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_sharedFlowFragment)
            }
        }
    }
}