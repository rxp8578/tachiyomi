package eu.kanade.tachiyomi.ui.reader.setting

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.domain.manga.model.orientationType
import eu.kanade.domain.manga.model.readingModeType
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderReadingModeSettingsBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.preference.bindToPreference
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.injectLazy

class ReaderSettingsSheet(
    private val activity: ReaderActivity,
) : BottomSheetDialog(activity) {

    private val readerPreferences: ReaderPreferences by injectLazy()

    private lateinit var binding: ReaderReadingModeSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ReaderReadingModeSettingsBinding.inflate(activity.layoutInflater)
        setContentView(binding.root)

        initGeneralPreferences()

        when (activity.viewModel.state.value.viewer) {
            is PagerViewer -> initPagerPreferences()
            is WebtoonViewer -> initWebtoonPreferences()
        }
    }

    private fun initGeneralPreferences() {
        binding.viewer.onItemSelectedListener = { position ->
            val readingModeType = ReadingModeType.fromSpinner(position)
            activity.viewModel.setMangaReadingMode(readingModeType.flagValue)

            val mangaViewer = activity.viewModel.getMangaReadingMode()
            if (mangaViewer == ReadingModeType.WEBTOON.flagValue || mangaViewer == ReadingModeType.CONTINUOUS_VERTICAL.flagValue) {
                initWebtoonPreferences()
            } else {
                initPagerPreferences()
            }
        }
        binding.viewer.setSelection(activity.viewModel.manga?.readingModeType?.let { ReadingModeType.fromPreference(it.toInt()).prefValue } ?: ReadingModeType.DEFAULT.prefValue)

        binding.rotationMode.onItemSelectedListener = { position ->
            val rotationType = OrientationType.fromSpinner(position)
            activity.viewModel.setMangaOrientationType(rotationType.flagValue)
        }
        binding.rotationMode.setSelection(activity.viewModel.manga?.orientationType?.let { OrientationType.fromPreference(it.toInt()).prefValue } ?: OrientationType.DEFAULT.prefValue)
    }

    private fun initPagerPreferences() {
        binding.webtoonPrefsGroup.root.isVisible = false
        binding.pagerPrefsGroup.root.isVisible = true

        binding.pagerPrefsGroup.tappingInverted.bindToPreference(readerPreferences.pagerNavInverted(), ReaderPreferences.TappingInvertMode::class.java)
        binding.pagerPrefsGroup.navigatePan.bindToPreference(readerPreferences.navigateToPan())

        binding.pagerPrefsGroup.pagerNav.bindToPreference(readerPreferences.navigationModePager())
        readerPreferences.navigationModePager().changes()
            .onEach {
                val isTappingEnabled = it != 5
                binding.pagerPrefsGroup.tappingInverted.isVisible = isTappingEnabled
                binding.pagerPrefsGroup.navigatePan.isVisible = isTappingEnabled
            }
            .launchIn(activity.lifecycleScope)
        // Makes so that landscape zoom gets hidden away when image scale type is not fit screen
        binding.pagerPrefsGroup.scaleType.bindToPreference(readerPreferences.imageScaleType(), 1)
        readerPreferences.imageScaleType().changes()
            .onEach { binding.pagerPrefsGroup.landscapeZoom.isVisible = it == 1 }
            .launchIn(activity.lifecycleScope)
        binding.pagerPrefsGroup.landscapeZoom.bindToPreference(readerPreferences.landscapeZoom())

        binding.pagerPrefsGroup.zoomStart.bindToPreference(readerPreferences.zoomStart(), 1)
        binding.pagerPrefsGroup.cropBorders.bindToPreference(readerPreferences.cropBorders())

        binding.pagerPrefsGroup.dualPageSplit.bindToPreference(readerPreferences.dualPageSplitPaged())
        readerPreferences.dualPageSplitPaged().changes()
            .onEach {
                binding.pagerPrefsGroup.dualPageInvert.isVisible = it
                if (it) {
                    binding.pagerPrefsGroup.dualPageRotateToFit.isChecked = false
                }
            }
            .launchIn(activity.lifecycleScope)
        binding.pagerPrefsGroup.dualPageInvert.bindToPreference(readerPreferences.dualPageInvertPaged())

        binding.pagerPrefsGroup.dualPageRotateToFit.bindToPreference(readerPreferences.dualPageRotateToFit())
        readerPreferences.dualPageRotateToFit().changes()
            .onEach {
                binding.pagerPrefsGroup.dualPageRotateToFitInvert.isVisible = it
                if (it) {
                    binding.pagerPrefsGroup.dualPageSplit.isChecked = false
                }
            }
            .launchIn(activity.lifecycleScope)
        binding.pagerPrefsGroup.dualPageRotateToFitInvert.bindToPreference(readerPreferences.dualPageRotateToFitInvert())
    }

    private fun initWebtoonPreferences() {
        binding.pagerPrefsGroup.root.isVisible = false
        binding.webtoonPrefsGroup.root.isVisible = true

        binding.webtoonPrefsGroup.tappingInverted.bindToPreference(readerPreferences.webtoonNavInverted(), ReaderPreferences.TappingInvertMode::class.java)

        binding.webtoonPrefsGroup.webtoonNav.bindToPreference(readerPreferences.navigationModeWebtoon())
        readerPreferences.navigationModeWebtoon().changes()
            .onEach { binding.webtoonPrefsGroup.tappingInverted.isVisible = it != 5 }
            .launchIn(activity.lifecycleScope)
        binding.webtoonPrefsGroup.cropBordersWebtoon.bindToPreference(readerPreferences.cropBordersWebtoon())
        binding.webtoonPrefsGroup.webtoonSidePadding.bindToIntPreference(readerPreferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)

        binding.webtoonPrefsGroup.dualPageSplit.bindToPreference(readerPreferences.dualPageSplitWebtoon())
        // Makes it so that dual page invert gets hidden away when dual page split is turned off
        readerPreferences.dualPageSplitWebtoon().changes()
            .onEach { binding.webtoonPrefsGroup.dualPageInvert.isVisible = it }
            .launchIn(activity.lifecycleScope)
        binding.webtoonPrefsGroup.dualPageInvert.bindToPreference(readerPreferences.dualPageInvertWebtoon())

        binding.webtoonPrefsGroup.longStripSplit.isVisible = !isReleaseBuildType
        binding.webtoonPrefsGroup.longStripSplit.bindToPreference(readerPreferences.longStripSplitWebtoon())

        binding.webtoonPrefsGroup.doubleTapZoom.bindToPreference(readerPreferences.webtoonDoubleTapZoomEnabled())
    }
}
