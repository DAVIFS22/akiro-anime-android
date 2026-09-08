package com.akiro.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akiro.addon.AddonManager
import com.akiro.addon.StreamDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * StreamsFragment: shows a list of streams for a given content id.
 * Usage: create the fragment and set arguments: manifestUrl, contentType (e.g. "series"/"movie"), contentId
 */
class StreamsFragment : Fragment() {
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var recycler: RecyclerView? = null
    private var progress: ProgressBar? = null
    private var adapter: StreamsAdapter? = null

    private var manifestUrl: String? = null
    private var contentType: String? = null
    private var contentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manifestUrl = arguments?.getString(ARG_MANIFEST_URL)
        contentType = arguments?.getString(ARG_TYPE)
        contentId = arguments?.getString(ARG_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(android.R.layout.simple_list_item_1, container, false)
        recycler = RecyclerView(requireContext())
        recycler?.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        recycler?.layoutManager = LinearLayoutManager(requireContext())
        adapter = StreamsAdapter(requireContext()) { stream ->
            // on click
            if (!stream.url.isNullOrEmpty()) {
                val i = Intent(requireContext(), com.akiro.player.PlayerActivity::class.java)
                i.putExtra("stream_url", stream.url)
                startActivity(i)
            } else if (!stream.magnet.isNullOrEmpty()) {
                val i = Intent(requireContext(), com.akiro.player.PlayerActivity::class.java)
                i.putExtra("magnet", stream.magnet)
                startActivity(i)
            } else {
                Toast.makeText(requireContext(), "Stream sem URL/magnet", Toast.LENGTH_SHORT).show()
            }
        }
        recycler?.adapter = adapter
        return recycler
    }

    override fun onResume() {
        super.onResume()
        loadStreams()
    }

    private fun loadStreams() {
        val manifest = manifestUrl
        val type = contentType ?: "movie"
        val id = contentId ?: return
        if (manifest == null) {
            Toast.makeText(requireContext(), "Manifest URL não configurado", Toast.LENGTH_SHORT).show()
            return
        }
        progress?.visibility = View.VISIBLE
        scope.launch {
            try {
                val manifestJson = withContext(Dispatchers.IO) { AddonManager.fetchManifest(manifest) }
                if (manifestJson == null) {
                    Toast.makeText(requireContext(), "Erro ao baixar manifest", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // Try to infer base url from manifest
                val baseUrl = manifest.substringBefore("/manifest.json") + "/"
                val api = AddonManager.createAddonApi(baseUrl)
                val streams: List<StreamDto> = api.getStreams(type, id)
                adapter?.submitList(streams)
            } catch (e: Exception) {
                Log.e("StreamsFragment", "erro ao carregar streams", e)
                Toast.makeText(requireContext(), "Erro ao carregar streams: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress?.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val ARG_MANIFEST_URL = "manifest_url"
        private const val ARG_TYPE = "content_type"
        private const val ARG_ID = "content_id"

        fun newInstance(manifestUrl: String, type: String, id: String): StreamsFragment {
            val f = StreamsFragment()
            val args = Bundle()
            args.putString(ARG_MANIFEST_URL, manifestUrl)
            args.putString(ARG_TYPE, type)
            args.putString(ARG_ID, id)
            f.arguments = args
            return f
        }
    }
}
