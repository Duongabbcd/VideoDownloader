package com.ezt.priv.shortvideodownloader.ui.downloadcard

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.AlreadyExistsItem
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.DownloadItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.HistoryViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.ResultViewModel
import com.ezt.priv.shortvideodownloader.ui.adapter.AlreadyExistsAdapter
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Extensions.enableFastScroll
import com.ezt.priv.shortvideodownloader.util.UiUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class DownloadsAlreadyExistBaseDialog : DialogFragment(), AlreadyExistsAdapter.OnItemClickListener {
    private var activity: Activity? = null
    private lateinit var downloadViewModel : DownloadViewModel
    private lateinit var resultViewModel : ResultViewModel
    private lateinit var historyViewModel : HistoryViewModel

    private var duplicateIDs : MutableList<DownloadViewModel.AlreadyExistsIDs> = mutableListOf()
    private lateinit var duplicates: MutableList<AlreadyExistsItem>
    private lateinit var preferences: SharedPreferences
    private lateinit var adapter: AlreadyExistsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = getActivity()
        downloadViewModel = ViewModelProvider(requireActivity())[DownloadViewModel::class.java]
        resultViewModel = ViewModelProvider(requireActivity())[ResultViewModel::class.java]
        historyViewModel = ViewModelProvider(requireActivity())[HistoryViewModel::class.java]
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        kotlin.runCatching {
            duplicateIDs = if (Build.VERSION.SDK_INT >= 33){
                arguments?.getParcelableArrayList("duplicates", DownloadViewModel.AlreadyExistsIDs::class.java)!!.toMutableList()
            }else{
                arguments?.getParcelableArrayList<DownloadViewModel.AlreadyExistsIDs>("duplicates")!!.toMutableList()
            }

            if (duplicateIDs.isEmpty()){
                dismiss()
            }
        }.onFailure {
            dismiss()
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())

        // Inflate your layout
        val view = requireActivity().layoutInflater.inflate(R.layout.fragment_already_exists_dialog, null)

        // Convert 16dp to pixels
        val marginInDp = 16
        val scale = resources.displayMetrics.density
        val marginInPx = (marginInDp * scale + 0.5f).toInt()

        // Set layout params with margins
        val params = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
        view.layoutParams = params

        dialog.setContentView(view)

        // Optional: transparent background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Optional: ensure dialog width wraps content minus margins
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Setup views, RecyclerView, buttons
        setupViews(view)

        return dialog
    }


    private fun setupViews(view: View) {
        // RecyclerView
        recyclerView = view.findViewById(R.id.downloadMultipleRecyclerview)
        adapter = AlreadyExistsAdapter(this, requireActivity())
        recyclerView.adapter = adapter
        recyclerView.enableFastScroll()
        recyclerView.gone()

        // Load duplicates
        loadDuplicates()

        // Button click
        view.findViewById<MaterialButton>(R.id.bottomsheet_download_button).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                downloadViewModel.deleteWithDuplicateStatus()
                val items = duplicates.map { it.downloadItem }
                items.forEach { it.id = 0 }
                val result = downloadViewModel.queueDownloads(items, true)
                if (result.message.isNotBlank()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                withContext(Dispatchers.Main){
                    dismiss()
                }
            }
        }
    }

    private fun loadDuplicates() {
        runBlocking {
            val items = withContext(Dispatchers.IO) {
                downloadViewModel.getAllByIDs(duplicateIDs.map { it.downloadItemID })
            }
            duplicates = items.map { item ->
                AlreadyExistsItem(
                    item,
                    duplicateIDs.firstOrNull { it.downloadItemID == item.id }?.historyItemID
                )
            }.toMutableList()
            adapter.submitList(duplicates.toList())
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        CoroutineScope(Dispatchers.IO).launch {
            if (existenceDismissedByUser) {
                // ✅ Dismissed by user
                Log.d("DownloadBottomSheet", "Dismissed by user")
                existenceDismissedByUser = false
            } else {
                // ❌ Dismissed programmatically (e.g. dismiss(), navigate(), etc.)
                Log.d("DownloadBottomSheet", "Dismissed programmatically")
            }
            resultViewModel.deleteAll(false)
            downloadViewModel.deleteWithDuplicateStatus()
        }
    }



    override fun onEditItem(alreadyExistsItem: AlreadyExistsItem, position: Int) {
        val onItemUpdated = object: ConfigureDownloadBottomSheetDialog.OnDownloadItemUpdateListener {
            override fun onDownloadItemUpdate(
                item: DownloadItem
            ) {
                val currentIndex = duplicates.indexOf(alreadyExistsItem)
                val current = duplicates[currentIndex]
                duplicates[currentIndex] = AlreadyExistsItem(item, current.historyID)
                adapter.submitList(duplicates)
                adapter.notifyItemChanged(position)
            }
        }
        val bottomSheet = ConfigureDownloadBottomSheetDialog(alreadyExistsItem.downloadItem, onItemUpdated)
        bottomSheet.show(requireActivity().supportFragmentManager, "configureDownloadSingleSheet")
    }

    override fun onDeleteItem(alreadyExistsItem: AlreadyExistsItem, position: Int) {
        UiUtil.showGenericDeleteDialog(requireContext(), alreadyExistsItem.downloadItem.title) {
            if (alreadyExistsItem.historyID == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    downloadViewModel.deleteDownload(alreadyExistsItem.downloadItem.id)
                }
            }
            duplicates.remove(alreadyExistsItem)
            if (duplicates.isEmpty()) {
                dismiss()
            }
            adapter.submitList(duplicates)
        }
    }

    override fun onShowHistoryItem(historyItemID: Long) {
        lifecycleScope.launch {
            val historyItem = withContext(Dispatchers.IO){
                downloadViewModel.getHistoryItemById(historyItemID)
            }
            UiUtil.showHistoryItemDetailsCard(historyItem, requireActivity(), isPresent = true, preferences,
                removeItem = { item, deleteFile ->
                    historyViewModel.delete(item, deleteFile)
                },
                redownloadItem = { },
                redownloadShowDownloadCard = {}
            )
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        existenceDismissedByUser = true
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
    }



    companion object {
        var existenceDismissedByUser: Boolean = false
    }
}