package com.ezt.priv.shortvideodownloader.ui.browse.module

import androidx.lifecycle.ViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.HistoryViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.SettingsViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.data.ViewModelKey
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.BrowserViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.MainViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.VideoDetectionTabViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.WebTabViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module(includes = [AppModule::class])
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {

//    @Singleton
//    @Binds
//    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
//
//    @Binds
//    @IntoMap
//    @ViewModelKey(SplashViewModel::class)
//    abstract fun bindSplashViewModel(viewModel: SplashViewModel): ViewModel
//
    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BrowserViewModel::class)
    abstract fun bindBrowserViewModel(viewModel: BrowserViewModel): ViewModel
//
//    @Binds
//    @IntoMap
//    @ViewModelKey(VideoPlayerViewModel::class)
//    abstract fun bindVideoPlayerViewModel(viewModel: VideoPlayerViewModel): ViewModel
//
//    @Binds
//    @IntoMap
//    @ViewModelKey(ProgressViewModel::class)
//    abstract fun bindProgressViewModel(viewModel: ProgressViewModel): ViewModel
//
//    @Binds
//    @IntoMap
//    @ViewModelKey(VideoViewModel::class)
//    abstract fun bindVideoViewModel(viewModel: VideoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HistoryViewModel::class)
    abstract fun bindHistoryViewModel(viewModel: HistoryViewModel): ViewModel

//    @Binds
//    @IntoMap
//    @ViewModelKey(ProxiesViewModel::class)
//    abstract fun bindProxiesViewModel(viewModel: ProxiesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WebTabViewModel::class)
    abstract fun bindWebTabViewModel(viewModel: WebTabViewModel): ViewModel

//    @Binds
//    @IntoMap
//    @ViewModelKey(BrowserHomeViewModel::class)
//    abstract fun bindBrowserHomeViewModel(viewModel: BrowserHomeViewModel): ViewModel
//
//    @Binds
//    @IntoMap
//    @ViewModelKey(GlobalVideoDetectionModel::class)
//    abstract fun bindVideoDetectionAlgViewModel(viewModel: GlobalVideoDetectionModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VideoDetectionTabViewModel::class)
    abstract fun bindVideoDetectionDetectedViewModel(viewModel: VideoDetectionTabViewModel): ViewModel
}
