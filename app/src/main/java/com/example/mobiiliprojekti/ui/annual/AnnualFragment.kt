package com.example.mobiiliprojekti.ui.annual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mobiiliprojekti.databinding.FragmentAnnualBinding

class AnnualFragment : Fragment() {

    private var _binding: FragmentAnnualBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val annualViewModel =
            ViewModelProvider(this).get(AnnualViewModel::class.java)

        _binding = FragmentAnnualBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textAnnual
        annualViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}