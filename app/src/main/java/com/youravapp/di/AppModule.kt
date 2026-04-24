package com.youravapp.di

import android.content.Context
import androidx.room.Room
import com.youravapp.data.datastore.SettingsStore
import com.youravapp.data.db.ScanDatabase
import com.youravapp.data.repository.AppRepositoryImpl
import com.youravapp.data.repository.PolicyRepositoryImpl
import com.youravapp.data.repository.QuarantineRepositoryImpl
import com.youravapp.data.repository.ScanRepositoryImpl
import com.youravapp.data.shizuku.SystemPackageProxy
import com.youravapp.domain.repository.IAppRepository
import com.youravapp.domain.repository.IPolicyRepository
import com.youravapp.domain.repository.IQuarantineRepository
import com.youravapp.domain.repository.IScanRepository
import com.youravapp.domain.usecase.EvaluatePolicyUseCase
import com.youravapp.domain.usecase.GetInstalledAppsUseCase
import com.youravapp.domain.usecase.ScanFilesUseCase
import com.youravapp.domain.usecase.ToggleComponentUseCase
import com.youravapp.domain.usecase.RunSandboxForNewAppUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context) = SettingsStore(context)

    @Provides @Singleton
    fun provideProxy(@ApplicationContext context: Context) = SystemPackageProxy(context)

    @Provides @Singleton
    fun provideScanRepository(@ApplicationContext context: Context, settingsStore: SettingsStore): IScanRepository =
        ScanRepositoryImpl(context, settingsStore)

    @Provides @Singleton
    fun provideAppRepository(proxy: SystemPackageProxy, settingsStore: SettingsStore): IAppRepository =
        AppRepositoryImpl(proxy, settingsStore)

    @Provides @Singleton
    fun provideQRepository(@ApplicationContext context: Context): IQuarantineRepository = QuarantineRepositoryImpl(context)

    @Provides @Singleton
    fun providePolicyRepository(): IPolicyRepository = PolicyRepositoryImpl()

    @Provides @Singleton
    fun provideDb(@ApplicationContext context: Context): ScanDatabase =
        Room.databaseBuilder(context, ScanDatabase::class.java, "scan.db").build()

    @Provides
    fun provideScanUseCase(
        scan: IScanRepository,
        quarantine: IQuarantineRepository,
        app: IAppRepository
    ) = ScanFilesUseCase(scan, quarantine, app)

    @Provides fun provideInstalledUseCase(app: IAppRepository) = GetInstalledAppsUseCase(app)
    @Provides fun provideToggleUseCase(app: IAppRepository) = ToggleComponentUseCase(app)
    @Provides fun provideSandboxUseCase(app: IAppRepository) = RunSandboxForNewAppUseCase(app)
    @Provides fun provideEvalPolicyUseCase(policy: IPolicyRepository) = EvaluatePolicyUseCase(policy)
}
