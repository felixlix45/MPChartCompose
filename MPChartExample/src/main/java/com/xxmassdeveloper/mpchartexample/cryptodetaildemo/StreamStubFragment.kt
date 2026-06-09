package com.xxmassdeveloper.mpchartexample.cryptodetaildemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xxmassdeveloper.mpchartexample.R

class StreamStubFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_stream_stub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerView>(R.id.rv_stream_stub)?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = StreamStubAdapter(createMockPosts())
            setHasFixedSize(true)
        }
    }

    private fun createMockPosts(): List<StreamPost> {
        return (1..40).map { index ->
            StreamPost(
                author = "Trader$index",
                message = "Mock stream post #$index about BTC — scroll this list while testing chart scrub above.",
            )
        }
    }

    private data class StreamPost(
        val author: String,
        val message: String,
    )

    private class StreamStubAdapter(
        private val items: List<StreamPost>,
    ) : RecyclerView.Adapter<StreamStubAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val author: TextView = view.findViewById(R.id.tv_stream_author)
            val message: TextView = view.findViewById(R.id.tv_stream_message)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stream_stub, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.author.text = item.author
            holder.message.text = item.message
        }

        override fun getItemCount(): Int = items.size
    }
}
