package br.com.guga.gravadorsuper.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import br.com.guga.gravadorsuper.models.Events
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import org.fossify.commons.extensions.appLaunched
import org.fossify.commons.extensions.checkAppSideloading
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.beGone
import org.fossify.commons.helpers.PERMISSION_RECORD_AUDIO
import org.fossify.commons.helpers.PERMISSION_WRITE_STORAGE
import org.fossify.commons.helpers.isRPlus
import br.com.guga.gravadorsuper.BuildConfig
import br.com.guga.gravadorsuper.R
import br.com.guga.gravadorsuper.adapters.ViewPagerAdapter
import br.com.guga.gravadorsuper.databinding.ActivityMainBinding
import br.com.guga.gravadorsuper.extensions.config
import br.com.guga.gravadorsuper.extensions.ensureStoragePermission
import br.com.guga.gravadorsuper.extensions.handlePermission
import org.greenrobot.eventbus.EventBus

class MainActivity : SimpleActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bus: EventBus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupOptionsMenu()
        refreshMenuItems()

        bus = EventBus.getDefault()
        bus?.register(this)

        handlePermission(PERMISSION_RECORD_AUDIO) {
            if (it) {
                tryInitVoiceRecorder()
            } else {
                toast(org.fossify.commons.R.string.no_audio_permissions)
                finish()
            }
        }

        checkAppSideloading()
        appLaunched(BuildConfig.APPLICATION_ID)
        
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    override fun onResume() {
        super.onResume()
        updateMenuColors()
    }

    private fun setupOptionsMenu() {
        binding.mainMenu.toolbar?.inflateMenu(R.menu.menu)
        binding.mainMenu.toggleHideOnScroll(false)
        // binding.mainMenu.setupWithViewPager(binding.viewPager)
        binding.mainMenu.onSearchOpenListener = {
            if (binding.viewPager.currentItem == 0) {
                binding.viewPager.currentItem = 1
            }
        }

        binding.mainMenu.onSearchTextChangedListener = { text ->
            getPagerAdapter()?.searchTextChanged(text)
        }

        binding.mainMenu.toolbar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete_all -> getPagerAdapter()?.deleteAll(binding.viewPager.currentItem)
                R.id.settings -> launchSettings()
                R.id.how_to -> launchHowTo()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateMenuColors() {
        binding.mainMenu.updateColors()
    }

    private fun tryInitVoiceRecorder() {
        if (isRPlus()) {
            ensureStoragePermission { granted ->
                if (granted) {
                    setupViewPager()
                } else {
                    toast(org.fossify.commons.R.string.no_storage_permissions)
                    finish()
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (it) {
                    setupViewPager()
                } else {
                    toast(org.fossify.commons.R.string.no_storage_permissions)
                    finish()
                }
            }
        }
    }

    private fun setupViewPager() {
        binding.mainTabsHolder.removeAllTabs()
        var tabDrawables = arrayOf(
            org.fossify.commons.R.drawable.ic_microphone_vector,
            R.drawable.ic_playlist_play_vector
        )
        var tabLabels = arrayOf(R.string.recorder, R.string.player)
        if (config.useRecycleBin) {
            tabDrawables += org.fossify.commons.R.drawable.ic_delete_vector
            tabLabels += org.fossify.commons.R.string.recycle_bin
        }

        tabDrawables.forEachIndexed { i, drawableId ->
            binding.mainTabsHolder.newTab()
                .setCustomView(org.fossify.commons.R.layout.bottom_tablayout_item).apply {
                    customView
                        ?.findViewById<ImageView>(org.fossify.commons.R.id.tab_item_icon)
                        ?.setImageDrawable(
                            AppCompatResources.getDrawable(
                                this@MainActivity,
                                drawableId
                            )
                        )
                    customView?.findViewById<TextView>(org.fossify.commons.R.id.tab_item_label)?.setText(tabLabels[i])
                    binding.mainTabsHolder.addTab(this)
                }
        }

        val adapter = ViewPagerAdapter(this, config.useRecycleBin)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                binding.mainTabsHolder.getTabAt(position)?.select()
                refreshMenuItems()
            }
            override fun onPageScrollStateChanged(state: Int) {}
        })

        binding.mainTabsHolder.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                binding.viewPager.currentItem = tab.position
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        binding.mainTabsHolder.getTabAt(0)?.select()

        if (isThirdPartyIntent()) {
            binding.mainTabsHolder.beGone()
            binding.viewPager.isEnabled = false
        }
    }

    private fun refreshMenuItems() {
        binding.mainMenu.toolbar?.menu?.apply {
            findItem(R.id.delete_all)?.isVisible = binding.viewPager.currentItem == 2
        }
    }

    private fun getPagerAdapter() = (binding.viewPager.adapter as? ViewPagerAdapter)

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        showInfoDialog(R.string.about, R.string.about_content)
    }

    private fun launchHowTo() {
        showInfoDialog(R.string.how_to, R.string.how_to_content)
    }

    private fun showInfoDialog(titleRes: Int, contentRes: Int) {
        getAlertDialogBuilder()
            .setTitle(titleRes)
            .setMessage(contentRes)
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .show()
    }

    private fun isThirdPartyIntent() = intent?.action == MediaStore.Audio.Media.RECORD_SOUND_ACTION

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun recordingSaved(event: Events.RecordingSaved) {
        if (isThirdPartyIntent()) {
            Intent().apply {
                data = event.uri!!
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                setResult(Activity.RESULT_OK, this)
            }
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.type?.startsWith("audio/") == true) {
            val uri = intent.data
            if (uri != null) {
                bus?.post(Events.PlayExternalAudio(uri))
            }
        }
    }
}
