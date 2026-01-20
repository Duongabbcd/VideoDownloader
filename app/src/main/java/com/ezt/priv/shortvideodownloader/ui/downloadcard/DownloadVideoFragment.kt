package com.ezt.priv.shortvideodownloader.ui.downloadcard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.expand.table.Format
import com.ezt.priv.shortvideodownloader.database.models.main.DownloadItem
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel.Type
import com.ezt.priv.shortvideodownloader.database.viewmodel.FormatViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.ResultViewModel
import com.ezt.priv.shortvideodownloader.databinding.FragmentDownloadVideoBinding
import com.ezt.priv.shortvideodownloader.ui.BaseFragment
import com.ezt.priv.shortvideodownloader.ui.adapter.FormatAdapter
import com.ezt.priv.shortvideodownloader.ui.downloadcard.FormatSelectionBottomSheetDialog
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.util.Extensions.applyFilenameTemplateForCuts
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.ezt.priv.shortvideodownloader.util.FormatUtil
import com.ezt.priv.shortvideodownloader.util.UiUtil
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
import com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.text.format

class DownloadVideoFragment(
    private var resultItem: ResultItem? = null,
    private var currentDownloadItem: DownloadItem? = null,
    private var url: String = "",
    private var nonSpecific: Boolean = false
) : BaseFragment<FragmentDownloadVideoBinding>(FragmentDownloadVideoBinding::inflate), GUISync {
    private var fragmentView: View? = null
    private var activity: Activity? = null
    private lateinit var downloadViewModel : DownloadViewModel
    private lateinit var formatViewModel : FormatViewModel
    private lateinit var resultViewModel: ResultViewModel
    private lateinit var preferences: SharedPreferences
    private lateinit var shownFields: List<String>

    private lateinit var genericVideoFormats: MutableList<Format>
    private lateinit var genericAudioFormats: MutableList<Format>

    lateinit var downloadItem: DownloadItem

    private var disabledCutClicked: Boolean = false

//    private lateinit var adapter: FormatAdapter
//    private lateinit var listener: OnFormatClickListener

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = getActivity()
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]
        formatViewModel = ViewModelProvider(requireActivity())[FormatViewModel::class.java]
        val formatUtil = FormatUtil(requireContext())
        genericVideoFormats = formatUtil.getGenericVideoFormats(requireContext().resources)
        genericAudioFormats = formatUtil.getGenericAudioFormats(requireContext().resources)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        shownFields = preferences.getStringSet(
            "modify_download_card",
            requireContext().resources.getStringArray(R.array.modify_download_card_values).toSet()
        )!!.toList()

        lifecycleScope.launch {

//            adapter =
//                FormatAdapter(
//                    this@DownloadVideoFragment,
//                    requireActivity()
//                )
//            binding.allVideoSolutions.layoutManager = LinearLayoutManager(requireContext(),
//                LinearLayoutManager.VERTICAL, false)
//            binding.allVideoSolutions.adapter = adapter
//            var formats = mutableListOf<Format>()
//            val formatCard = view.findViewById<MaterialCardView>(R.id.format_card_constraintLayout)
//            listener = object : OnFormatClickListener {
//                override fun onFormatClick(formatTuple: FormatTuple) {
//                    formatTuple.format?.apply {
//                        downloadItem.format = this
//                    }
//                    downloadItem.videoPreferences.audioFormatIDs.clear()
//                    formatTuple.audioFormats?.map { it.format_id }?.let {
//                        downloadItem.videoPreferences.audioFormatIDs.addAll(it)
//                    }
//                    val filesize = UiUtil.populateFormatCard(requireContext(), formatCard, downloadItem.format,
//                        if(downloadItem.videoPreferences.removeAudio) listOf() else formatTuple.audioFormats,
//                        showSize = downloadItem.downloadSections.isEmpty()
//                    )
//                    formatViewModel.checkFreeSpace(filesize, downloadItem.downloadPath)
//                }
//
//                override fun onFormatsUpdated(allFormats: List<Format>) {
//                    lifecycleScope.launch {
//                        withContext(Dispatchers.IO){
//                            resultItem?.apply {
//                                this.formats.removeAll(formats)
//                                this.formats.addAll(allFormats.filter { !genericVideoFormats.contains(it) })
//                                resultViewModel.update(this)
//                            }
//
//                            currentDownloadItem?.apply {
//                                downloadViewModel.updateDownloadItemFormats(this.id, allFormats.filter { !genericVideoFormats.contains(it) })
//                            }
//                        }
//                    }
//                    formats = allFormats.filter { !genericVideoFormats.contains(it) }.toMutableList()
//                    val preferredFormat = downloadViewModel.getFormat(formats, Type.video)
//                    val preferredAudioFormats = downloadViewModel.getPreferredAudioFormats(formats)
//                    downloadItem.format = preferredFormat
//                    downloadItem.allFormats = formats
//                    val filesize = UiUtil.populateFormatCard(requireContext(), formatCard, preferredFormat,
//                        if(downloadItem.videoPreferences.removeAudio) listOf() else formats.filter { preferredAudioFormats.contains(it.format_id) },
//                        showSize = downloadItem.downloadSections.isEmpty()
//                    )
//                    formatViewModel.checkFreeSpace(filesize, downloadItem.downloadPath)
//                }
//            }


            downloadItem = withContext(Dispatchers.IO){
                if (savedInstanceState?.containsKey("updated") == true) {
                    downloadItem.apply {
                        title = resultItem!!.title
                        author = resultItem!!.author
                        allFormats = resultItem!!.formats

                        val originFormat = downloadViewModel.getFormat(allFormats, Type.audio)
                        format = originFormat.toSafe()

                        duration = resultItem!!.duration
                        playlistIndex = resultItem!!.playlistIndex
                        playlistURL = resultItem!!.playlistURL
                        playlistTitle = resultItem!!.playlistTitle
                        thumb = resultItem!!.thumb
                        website = resultItem!!.website
                        url = resultItem!!.url
                        videoPreferences.apply {
                            audioFormatIDs = downloadViewModel.getPreferredAudioFormats(allFormats)
                        }
                    }
                }else if (currentDownloadItem != null){
                    val string = Gson().toJson(currentDownloadItem, DownloadItem::class.java)
                    Gson().fromJson(string, DownloadItem::class.java)
                }else{
                    downloadViewModel.createDownloadItemFromResult(resultItem, url, Type.video)
                }
            }

            binding.moreOption.isVisible = downloadItem.allFormats.size > 1

//            formatViewModel.setItem(downloadItem, !nonSpecific)
//            formatViewModel.formats.collectLatest {
//                if (it.isEmpty()) {
//                    binding.allVideoSolutions.gone()
//                    return@collectLatest
//                }
//                it.onEach { format ->
//                    println("Download Video: $format")
//                }
//                adapter.setCanMultiSelectAudio(formatViewModel.canMultiSelectAudio.value)
//                adapter.submitList(it.toMutableList())
//            }


            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            try {
                binding.titleTextinput.visibility = if (shownFields.contains("title") && !nonSpecific) View.VISIBLE else View.GONE
                if (binding.titleTextinput.editText?.text?.isEmpty() == true){
                    binding.titleTextinput.editText!!.setText(downloadItem.title)
                }
                binding.clearText.setOnClickListener {
                    binding.titleTextinput.editText!!.setText("")
                }

                binding.titleTextinput.editText!!.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(p0: Editable?) {
                        downloadItem.title = p0.toString()
                    }
                })
                
                binding.authorTextinput.visibility =  View.GONE
                if (binding.authorTextinput.editText?.text?.isEmpty() == true){
                    binding.authorTextinput.editText!!.setText(downloadItem.author)
                    binding.authorTextinput.endIconMode = END_ICON_NONE
                }
                binding.authorTextinput.editText!!.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(p0: Editable?) {
                        downloadItem.author = p0.toString()
                    }
                })

                if (savedInstanceState?.containsKey("updated") == true){
                    if (!listOf(resultItem?.title, downloadItem.title).contains(binding.titleTextinput.editText?.text.toString())){
                        binding.titleTextinput.endIconDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_refresh)
                        downloadItem.title = binding.titleTextinput.editText?.text.toString()
                    }

                    if (!listOf(resultItem?.author, downloadItem.author).contains(binding.authorTextinput.editText?.text.toString())){
                        binding.authorTextinput.endIconMode = END_ICON_CUSTOM
                        binding.authorTextinput.endIconDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_refresh)
                        downloadItem.author = binding.authorTextinput.editText?.text.toString()
                    }
                }


                binding.authorTextinput.setEndIconOnClickListener {
                    if (resultItem != null){
                        binding.authorTextinput.editText?.setText(resultItem?.author)
                    }
                    binding.authorTextinput.endIconMode = END_ICON_NONE
                }
                
                binding.outputPath.editText!!.setText(
                    FileUtil.formatPath(downloadItem.downloadPath)
                )
                 binding.outputPath.editText!!.isFocusable = false
                 binding.outputPath.editText!!.isClickable = true
                 binding.outputPath.editText!!.setOnClickListener {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    pathResultLauncher.launch(intent)
                }

                val free = FileUtil.convertFileSize(
                    File(FileUtil.formatPath(downloadItem.downloadPath)).freeSpace)
                binding.freespace.text = String.format( getString(R.string.freespace) + ": " + free)
                if (free == "?") binding.freespace.visibility = View.GONE


                var formats = mutableListOf<Format>()
                if (currentDownloadItem == null) {
                    formats.addAll(resultItem?.formats ?: listOf())
                }else{
                    //if its updating a present downloaditem and its the wrong category
                    if (currentDownloadItem!!.type != Type.video){
                        downloadItem.type = Type.video
                        runCatching {
                            downloadItem.format = downloadViewModel.getFormat(downloadItem.allFormats, Type.video)
                            if (downloadItem.videoPreferences.audioFormatIDs.isEmpty()){
                                downloadItem.videoPreferences.audioFormatIDs.add(
                                    downloadViewModel.getFormat(downloadItem.allFormats, Type.audio).format_id
                                )
                            }
                        }.onFailure {
                            downloadItem.format = genericVideoFormats.last()
                        }
                    }
                }
                if (formats.isEmpty()) formats.addAll(downloadItem.allFormats)

                val containers = requireContext().resources.getStringArray(R.array.video_containers)
                val container = view.findViewById<TextInputLayout>(R.id.downloadContainer)
                container.visibility = if (shownFields.contains("container")) View.GONE else View.GONE
                if (nonSpecific){
                    val param = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1.0f
                    )
                    container.layoutParams = param
                    container.setPadding(0)
                }
                val containerAutoCompleteTextView =
                    view.findViewById<AutoCompleteTextView>(R.id.container_textview)
                var containerPreference = sharedPreferences.getString("video_format", "Default")
                if (containerPreference == "Default") containerPreference = getString(R.string.defaultValue)

                if (formats.isEmpty()) formats = genericVideoFormats

                val formatCard = view.findViewById<MaterialCardView>(R.id.format_card_constraintLayout)
                formatCard.visible()
                val chosenFormat = downloadItem.format
                val chosenAudioFormats = downloadItem.allFormats.filter { downloadItem.videoPreferences.audioFormatIDs.contains(it.format_id) }
                val filesize = UiUtil.populateFormatCard(
                    requireContext(),
                    formatCard,
                    chosenFormat,
                    chosenAudioFormats,
                    showSize = downloadItem.downloadSections.isEmpty()
                )
                formatViewModel.checkFreeSpace(filesize, downloadItem.downloadPath)

                val listener = object : OnFormatClickListener {
                    override fun onFormatClick(formatTuple: FormatTuple) {
                        formatTuple.format?.apply {
                            downloadItem.format = this
                        }
                        downloadItem.videoPreferences.audioFormatIDs.clear()
                        formatTuple.audioFormats?.map { it.format_id }?.let {
                            downloadItem.videoPreferences.audioFormatIDs.addAll(it)
                        }
                        val filesize = UiUtil.populateFormatCard(requireContext(), formatCard, downloadItem.format,
                            if(downloadItem.videoPreferences.removeAudio) listOf() else formatTuple.audioFormats,
                            showSize = downloadItem.downloadSections.isEmpty()
                        )
                        formatViewModel.checkFreeSpace(filesize, downloadItem.downloadPath)
                    }

                    override fun onFormatsUpdated(allFormats: List<Format>) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO){
                                resultItem?.apply {
                                    this.formats.removeAll(formats)
                                    this.formats.addAll(allFormats.filter { !genericVideoFormats.contains(it) })
                                    resultViewModel.update(this)
                                }

                                currentDownloadItem?.apply {
                                    downloadViewModel.updateDownloadItemFormats(this.id, allFormats.filter { !genericVideoFormats.contains(it) })
                                }
                            }
                        }
                        formats = allFormats.filter { !genericVideoFormats.contains(it) }.toMutableList()
                        val preferredFormat = downloadViewModel.getFormat(formats, Type.video)
                        val preferredAudioFormats = downloadViewModel.getPreferredAudioFormats(formats)
                        downloadItem.format = preferredFormat
                        downloadItem.allFormats = formats
                        val filesize = UiUtil.populateFormatCard(requireContext(), formatCard, preferredFormat,
                            if(downloadItem.videoPreferences.removeAudio) listOf() else formats.filter { preferredAudioFormats.contains(it.format_id) },
                            showSize = downloadItem.downloadSections.isEmpty()
                        )
                        formatViewModel.checkFreeSpace(filesize, downloadItem.downloadPath)
                    }

                }
                formatCard.setOnClickListener{
                    if(resultItem!!.formats.size == 1) {
                        return@setOnClickListener
                    }

                    if (parentFragmentManager.findFragmentByTag("formatSheet") == null){
                        formatViewModel.setItem(downloadItem, !nonSpecific)
                        val bottomSheet = FormatSelectionBottomSheetDialog(listener)
                        bottomSheet.show(parentFragmentManager, "formatSheet")
                    }
                }

                binding.moreOption.setOnClickListener {
                    if (parentFragmentManager.findFragmentByTag("formatSheet") == null){
                        formatViewModel.setItem(downloadItem, !nonSpecific)
                        val bottomSheet = FormatSelectionBottomSheetDialog(listener)
                        bottomSheet.show(parentFragmentManager, "formatSheet")
                    }
                }

                formatCard.setOnLongClickListener {
                    UiUtil.showFormatDetails(downloadItem.format, requireActivity(), false)
                    true
                }

                container?.isEnabled = !downloadItem.videoPreferences.compatibilityMode
                containerAutoCompleteTextView?.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        containers
                    )
                )
                if (currentDownloadItem == null || !containers.contains(downloadItem.container.ifEmpty { getString(R.string.defaultValue) })){
                    downloadItem.container = if (containerPreference == getString(R.string.defaultValue)) "" else containerPreference!!
                }
                if (downloadItem.videoPreferences.compatibilityMode) {
                    container.editText?.setText("mkv")
                }
                containerAutoCompleteTextView!!.setText(
                    downloadItem.container.ifEmpty { getString(R.string.defaultValue) },
                    false)

                (container!!.editText as AutoCompleteTextView?)!!.onItemClickListener =
                    AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, index: Int, _: Long ->
                        downloadItem.container = containers[index]
                        if (containers[index] == getString(R.string.defaultValue)) downloadItem.container = ""
                        if (containers[index] == "gif") {
                            view.findViewById<Chip>(R.id.adjust_audio).isEnabled = false
                            downloadItem.videoPreferences.removeAudio = true
                            view.findViewById<Chip>(R.id.recode_video).isEnabled = false
                            downloadItem.videoPreferences.recodeVideo = true
                        }else {
                            view.findViewById<Chip>(R.id.adjust_audio).isEnabled = true
                            downloadItem.videoPreferences.removeAudio = false
                            view.findViewById<Chip>(R.id.recode_video).isEnabled = true
                            downloadItem.videoPreferences.recodeVideo = false
                        }
                    }

                view.findViewById<LinearLayout>(R.id.adjust).apply {
                    visibility = if (shownFields.contains("adjust_video")) View.GONE else View.GONE
                    if (isVisible){
                        UiUtil.configureVideo(
                            view,
                            requireActivity(),
                            listOf(downloadItem),
                            embedSubsClicked = {
                                downloadItem.videoPreferences.embedSubs = it
                            },
                            addChaptersClicked = {
                                downloadItem.videoPreferences.addChapters = it
                            },
                            splitByChaptersClicked = {
                                downloadItem.videoPreferences.splitByChapters = it
                            },
                            embedThumbnailClicked = {
                                downloadItem.videoPreferences.embedThumbnail = it
                            },
                            saveThumbnailClicked = {
                                downloadItem.SaveThumb = it
                            },
                            sponsorBlockItemsSet = { values, checkedItems ->
                                downloadItem.videoPreferences.sponsorBlockFilters.clear()
                                for (i in checkedItems.indices) {
                                    if (checkedItems[i]) {
                                        downloadItem.videoPreferences.sponsorBlockFilters.add(values[i])
                                    }
                                }
                            },
                            cutClicked = { cutVideoListener ->
                                if (parentFragmentManager.findFragmentByTag("cutVideoSheet") == null){
                                    val bottomSheet = CutVideoBottomSheetDialog(downloadItem, resultItem?.urls ?: "", resultItem?.chapters ?: listOf(), cutVideoListener)
                                    bottomSheet.show(parentFragmentManager, "cutVideoSheet")
                                }
                            },
                            cutDisabledClicked = {
                                val isUpdatingData = ViewModelProvider(requireActivity())[ResultViewModel::class.java].updatingData.value
                                if(isUpdatingData){
                                    val snack = Snackbar.make(view, context.getString(R.string.please_wait), Snackbar.LENGTH_SHORT)
                                    snack.show()
                                }else if (downloadItem.duration == "0:00" || downloadItem.duration == "-1"){
                                    val snack = Snackbar.make(view, context.getString(R.string.cut_unsupported), Snackbar.LENGTH_SHORT)
                                    snack.show()
                                }else if (!nonSpecific){
                                    val snack = Snackbar.make(view, context.getString(R.string.cut_unavailable_please_update_item), Snackbar.LENGTH_SHORT)
                                    snack.setAction(R.string.update){
                                        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                                            resultItem?.apply {
                                                val rsVM = ViewModelProvider(requireActivity())[ResultViewModel::class.java]
                                                rsVM.updateItemData(this)
                                                disabledCutClicked = true
                                            }
                                        }
                                    }
                                    snack.show()
                                }
                            },
                            cutValueChanged = {
                                downloadItem.downloadSections = it
                                val filesize = UiUtil.populateFormatCard(
                                    requireContext(),
                                    formatCard,
                                    downloadItem.format,
                                    if(downloadItem.videoPreferences.removeAudio) listOf()
                                    else downloadItem.allFormats.filter { f -> downloadItem.videoPreferences.audioFormatIDs.contains(f.format_id) },
                                    showSize = downloadItem.downloadSections.isEmpty()
                                )
                                formatViewModel.checkFreeSpace(filesize, downloadItem.downloadPath)

                                if (it.isNotBlank()){
                                    downloadItem.customFileNameTemplate = downloadItem.customFileNameTemplate.applyFilenameTemplateForCuts()
                                }else{
                                    downloadItem.customFileNameTemplate = sharedPreferences.getString("file_name_template", "%(uploader).30B - %(title).170B")!!
                                }
                            },
                            filenameTemplateSet = {
                                downloadItem.customFileNameTemplate = it
                            },
                            saveSubtitlesClicked = {
                                downloadItem.videoPreferences.writeSubs = it
                            },
                            saveAutoSubtitlesClicked = {
                                downloadItem.videoPreferences.writeAutoSubs = it
                            },
                            subtitleLanguagesSet = {
                                downloadItem.videoPreferences.subsLanguages = it
                            },
                            removeAudioClicked = {
                                downloadItem.videoPreferences.removeAudio = it
                                val filesize = UiUtil.populateFormatCard(
                                    requireContext(),
                                    formatCard,
                                    downloadItem.format,
                                    if (it) listOf() else downloadItem.allFormats.filter { downloadItem.videoPreferences.audioFormatIDs.contains(it.format_id) },
                                    showSize = downloadItem.downloadSections.isEmpty()
                                )
                                formatViewModel.checkFreeSpace(filesize, downloadItem.downloadPath)
                            },
                            recodeVideoClicked = {
                                downloadItem.videoPreferences.recodeVideo = it
                            },
                            compatibilityModeClicked = {
                                downloadItem.videoPreferences.compatibilityMode = it
                                container.isEnabled = !it
                                if (it) {
                                    containerAutoCompleteTextView.setText("mkv",false)
                                    downloadItem.container = "mkv"
                                }
                            },
                            alsoDownloadAsAudioClicked = {
                                downloadItem.videoPreferences.alsoDownloadAsAudio = it
                            },
                            extraCommandsClicked = { returnValue ->
                                val callback = object : ExtraCommandsListener {
                                    override fun onChangeExtraCommand(c: String) {
                                        downloadItem.extraCommands = c
                                        returnValue(c)
                                    }
                                }

                                val bottomSheetDialog = AddExtraCommandsDialog(downloadItem, callback)
                                bottomSheetDialog.show(parentFragmentManager, "extraCommands")
                            },
                            liveFromStart = {
                                downloadItem.videoPreferences.liveFromStart = it
                            },
                            waitForVideo = { wait, value ->
                                downloadItem.videoPreferences.waitForVideoMinutes = if (wait) value else 0
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            view.findViewById<Chip>(R.id.cut).apply {
                if (this.isEnabled && savedInstanceState?.containsKey("click_cut") == true) {
                    this.performClick()
                }
            }
        }

        lifecycleScope.launch {
            formatViewModel.noFreeSpace.collectLatest {
                if (it != null) {
                    val snack = Snackbar.make(view, it, Snackbar.LENGTH_INDEFINITE)
                    snack.setTextMaxLines(10)
                    snack.show()
                }
            }
        }
    }

    override fun updateTitleAuthor(t: String, a: String){
        downloadItem.title = t
        downloadItem.author = a
        binding.titleTextinput.editText?.setText(t)
        binding.titleTextinput.endIconMode = END_ICON_NONE
        binding.authorTextinput.editText?.setText(a)
        binding.titleTextinput.endIconMode = END_ICON_NONE
    }

    override fun updateUI(res: ResultItem?) {
        resultItem = res
        val state = Bundle()
        state.putBoolean("updated", true)
        if (disabledCutClicked) {
            state.putBoolean("click_cut", true)
            disabledCutClicked = false
        }
        onViewCreated(requireView(),savedInstanceState = state)
    }

    @SuppressLint("RestrictedApi")
    fun updateSelectedAudioFormat(format: Format){
        if (downloadItem.videoPreferences.audioFormatIDs.contains(format.format_id)) {
            return
        }

        downloadItem.videoPreferences.audioFormatIDs.clear()
        downloadItem.videoPreferences.audioFormatIDs.addAll(arrayListOf(format.format_id))
        val formatCard = requireView().findViewById<MaterialCardView>(R.id.format_card_constraintLayout)
        val filesize = UiUtil.populateFormatCard(requireContext(), formatCard, downloadItem.format,
            if(downloadItem.videoPreferences.removeAudio) listOf() else listOf(format),
            showSize = downloadItem.downloadSections.isEmpty()
        )
        formatViewModel.checkFreeSpace(filesize, downloadItem.downloadPath)
    }

    private var pathResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                activity?.contentResolver?.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }

            downloadItem.downloadPath = result.data?.data.toString()
             binding.outputPath.editText?.setText(FileUtil.formatPath(result.data?.data.toString()), TextView.BufferType.EDITABLE)

            val free = FileUtil.convertFileSize(
                File(FileUtil.formatPath(downloadItem.downloadPath)).freeSpace)
            binding.freespace.text = String.format( getString(R.string.freespace) + ": " + free)
            if (free == "?") binding.freespace.visibility = View.GONE

        }
    }

//    override fun onItemSelect(
//        item: Format,
//        audioFormats: List<Format>?
//    ) {
//        listener.onFormatClick(FormatTuple(item, audioFormats))
//    }

}