/*
 * Copyright 2021 Michael Moessner
 *
 * This file is part of Metronome.
 *
 * Metronome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metronome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Metronome.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.metronome

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2

class MetronomeAndScenesFragment : Fragment() {

    private val metronomeViewModel by activityViewModels<MetronomeViewModel> {
        val playerConnection = PlayerServiceConnection.getInstance(
                requireContext(),
                AppPreferences.readMetronomeBpm(requireActivity()),
                AppPreferences.readMetronomeNoteList(requireActivity())
        )
        MetronomeViewModel.Factory(playerConnection)
    }

    private val scenesViewModel by activityViewModels<ScenesViewModel> {
        ScenesViewModel.Factory(AppPreferences.readScenesDatabase(requireActivity()))
    }

    var viewPager: ViewPager2? = null
        private set

    private var actionMode: ActionMode? = null

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            (activity as MainActivity?)?.setDisplayHomeButton()
            metronomeViewModel.isVisible = (position == ViewPagerAdapter.METRONOME)
            scenesViewModel.isVisible = (position == ViewPagerAdapter.SCENES)

            super.onPageSelected(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            when(state) {
                ViewPager2.SCROLL_STATE_SETTLING -> metronomeViewModel.setParentViewPagerSwiping(false)
                ViewPager2.SCROLL_STATE_DRAGGING -> metronomeViewModel.setParentViewPagerSwiping(true)
                ViewPager2.SCROLL_STATE_IDLE -> metronomeViewModel.setParentViewPagerSwiping(false)
            }
            super.onPageScrollStateChanged(state)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.metronome_and_scenes, menu)
        // super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean {
        when (item.itemId) {
            R.id.action_load -> {
                viewPager?.currentItem = ViewPagerAdapter.SCENES
                return true
            }
            R.id.action_edit -> {
                metronomeViewModel.setEditedSceneTitle(null)
                val activeStableId = scenesViewModel.activeStableId.value ?: Scene.NO_STABLE_ID
                val sceneTitle = scenesViewModel.scenes.value?.getScene(activeStableId)?.title
                metronomeViewModel.setEditedSceneTitle(sceneTitle)
                startEditingMode()
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Log.v("Metronome", "MetronomeAndScenesFragment:onCreateView")
        val view = inflater.inflate(R.layout.fragment_metronome_and_scenes, container, false)

        viewPager = view.findViewById(R.id.viewpager)
        viewPager?.adapter = ViewPagerAdapter(requireActivity())

        (activity as MainActivity?)?.setDisplayHomeButton()

        viewPager?.registerOnPageChangeCallback(pageChangeListener)
        viewPager?.offscreenPageLimit = 1

        scenesViewModel.editingStableId.observe(viewLifecycleOwner) {
            lockViewPager()
        }

        scenesViewModel.uri.observe(viewLifecycleOwner) {
            if (it != null) {
                actionMode?.finish()
                actionMode = null
//                Log.v("Metronome", "MetronomeAndScenesFragment: observing uri: uri=$it")
                // use "post" here, since otherwise, the view pager might miss the scene switching
                viewPager?.post {viewPager?.currentItem = ViewPagerAdapter.SCENES}
                scenesViewModel.loadingFileComplete(ScenesViewModel.FragmentTypes.MetronomeAndScenes)
            }
        }

        if (scenesViewModel.editingStableId.value != Scene.NO_STABLE_ID)
            startEditingMode()
        return view
    }

    override fun onDestroyView() {
        viewPager?.unregisterOnPageChangeCallback(pageChangeListener)
        super.onDestroyView()
    }

    private fun startEditingMode() {
        actionMode = (requireActivity() as MainActivity).startSupportActionMode(
                EditSceneCallback(requireActivity() as MainActivity, scenesViewModel, metronomeViewModel)
        )
        actionMode?.title = getString(R.string.editing_scene)
        val stableId = scenesViewModel.activeStableId.value
//                Log.v("Metronome", "MainActivity: onOptionsItemSelected: R.id.action_edit, stableId = $stableId")
        if (stableId != null && stableId != Scene.NO_STABLE_ID) {
            scenesViewModel.setEditingStableId(stableId)
            viewPager?.currentItem = ViewPagerAdapter.METRONOME
        }
    }

    private fun lockViewPager() {
        var lock = false

        scenesViewModel.editingStableId.value?.let {
            if (it != Scene.NO_STABLE_ID)
                lock = true
        }

        viewPager?.isUserInputEnabled = !lock
    }

}