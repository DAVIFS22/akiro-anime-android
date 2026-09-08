package com.akiro.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.akiro.addon.AddonManager
import com.akiro.addon.StreamDto
import com.akiro.player.PlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StreamsFragment : Fragment() {
    private val scope = CoroutineScope(Job() + Dispatchers.Main.immediate)
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
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val recycler = RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(requireContext())
            adapter = StreamsAdapter(requireContext(), ::openStream)
        }
        adapter = recycler.adapter as StreamsAdapter
        return recycler
    }

    override fun onResume() {
        super.onResume()
        loadStreams()
    }

    private fun openStream(stream: StreamDto) {
        val intent = Intent(requireContext(), PlayerActivity::class.java)
        when {
            !stream.url.isNullOrBlank() -> intent.putExtra("stream_url", stream.url)
            !stream.magnet.isNullOrBlank() -> intent.putExtra("magnet", stream.magnet)
            else -> {
                Toast.makeText(requireContext(), "Stream sem URL/magnet", Toast.LENGTH_SHORT).show()
                return
            }
        }
        startActivity(intent)
    }

    private fun loadStreams() {
        val manifest = manifestUrl
        val type = contentType ?: "movie"
        val id = contentId ?: return
        if (manifest.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Manifest URL não configurado", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                val manifestJson = withContext(Dispatchers.IO) {
                    AddonManager.fetchManifest(manifest)
                }
                if (manifestJson == null) {
                    Toast.makeText(requireContext(), "Erro ao baixar manifest", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val baseUrl = manifest.substringBeforeLast("/manifest.json") + "/"
                val response = withContext(Dispatchers.IO) {
                    AddonManager.createAddonApi(baseUrl).getStreams(type, id)
                }
                adapter?.submitList(response.streams)
            } catch (e: Exception) {
                Log.e("StreamsFragment", "erro ao carregar streams", e)
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar streams: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ARG_MANIFEST_URL = "manifest_url"
        private const val ARG_TYPE = "content_type"
        private const val ARG_ID = "content_id"

        fun newInstance(manifestUrl: String, type: String, id: String) =
            StreamsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MANIFEST_URL, manifestUrl)
                    putString(ARG_TYPE, type)
                    putString(ARG_ID, id)
                }
            }
    }
}
