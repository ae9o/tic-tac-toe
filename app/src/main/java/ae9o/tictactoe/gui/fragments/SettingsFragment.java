/*
 * Copyright (C) 2022 Alexei Evdokimenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ae9o.tictactoe.gui.fragments;

import ae9o.tictactoe.R;
import ae9o.tictactoe.databinding.FragmentSettingsBinding;
import ae9o.tictactoe.gui.MainViewModel;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

/**
 * Fragment with game settings. Observes the {@link MainViewModel} settings and pushes them to the UI. Also, when
 * changing settings in the UI, stores them back to the {@link MainViewModel}.
 */
public class SettingsFragment extends Fragment {
    /** Binding with views from the Fragment's xml resource. */
    private FragmentSettingsBinding binding;
    /** Model to store settings. */
    private MainViewModel viewModel;

    /**
     * Inflates the instance with views from the Fragment's xml resource.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return the View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes the instance once its view hierarchy has been completely created.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LifecycleOwner lifecycleOwner = getViewLifecycleOwner();

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getAiEnabled().observe(lifecycleOwner, this::onAiEnabledChanged);
        viewModel.getAiStarts().observe(lifecycleOwner, this::onAiStartsChanged);
        viewModel.getSwapMarks().observe(lifecycleOwner, this::onSwapMarksChanged);
        viewModel.getFieldSize().observe(lifecycleOwner, this::onFieldSizeChanged);

        binding.aiEnabledSwitch.setOnCheckedChangeListener(this::onAiEnabledSwitchChanged);
        binding.aiStartsSwitch.setOnCheckedChangeListener(this::onAiStartsSwitchChanged);
        binding.swapMarksSwitch.setOnCheckedChangeListener(this::onSwapMarksSwitchChanged);

        binding.fieldSizeBar.setMax(MainViewModel.MAX_GAME_FIELD_SIZE - MainViewModel.MIN_GAME_FIELD_SIZE);
        binding.fieldSizeBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // Do nothing.
                    }
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Do nothing.
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        onFieldSizeBarChanged(seekBar, progress, fromUser);
                    }
                }
        );
    }

    /**
     * Disposes fragment's resources.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Observes the {@link MainViewModel#getAiEnabled()} setting and pushes it to the UI.
     *
     * @param aiEnabled Current setting value.
     */
    private void onAiEnabledChanged(boolean aiEnabled) {
        binding.aiEnabledSwitch.setChecked(aiEnabled);
    }

    /**
     * Sends the value of the "aiEnabled" setting to the {@link MainViewModel} when it is changed in the UI.
     *
     * @param button The UI control used to change the value.
     * @param isChecked Current setting value.
     */
    private void onAiEnabledSwitchChanged(View button, boolean isChecked) {
        viewModel.setAiEnabled(isChecked);
    }

    /**
     * Observes the {@link MainViewModel#getAiStarts()} setting and pushes it to the UI.
     *
     * @param aiStarts Current setting value.
     */
    private void onAiStartsChanged(boolean aiStarts) {
        binding.aiStartsSwitch.setChecked(aiStarts);
    }

    /**
     * Sends the value of the "aiStarts" setting to the {@link MainViewModel} when it is changed in the UI.
     *
     * @param button The UI control used to change the value.
     * @param isChecked Current setting value.
     */
    private void onAiStartsSwitchChanged(View button, boolean isChecked) {
        viewModel.setAiStarts(isChecked);
    }

    /**
     * Observes the {@link MainViewModel#getSwapMarks()} setting and pushes it to the UI.
     *
     * @param swapMarks Current setting value.
     */
    private void onSwapMarksChanged(boolean swapMarks) {
        binding.swapMarksSwitch.setChecked(swapMarks);
    }

    /**
     * Sends the value of the "swapMarks" setting to the {@link MainViewModel} when it is changed in the UI.
     *
     * @param button UI The UI control used to change the value.
     * @param isChecked Current setting value.
     */
    private void onSwapMarksSwitchChanged(View button, boolean isChecked) {
        viewModel.setSwapMarks(isChecked);
    }

    /**
     * Observes the {@link MainViewModel#getFieldSize()} setting and pushes it to the UI.
     *
     * @param size Current setting value.
     */
    private void onFieldSizeChanged(int size) {
        binding.fieldSizeText.setText(getString(R.string.settings_field_size_text, size));
        binding.fieldSizeBar.setProgress(size - MainViewModel.MIN_GAME_FIELD_SIZE);
    }

    /**
     * Sends the value of the "fieldSize" setting to the {@link MainViewModel} when it is changed in the UI.
     *
     * @param seekBar The UI control used to change the value.
     * @param progress Current setting value.
     * @param fromUser true if the user changed the value;
     *                 false if the change is programmatic.
     */
    private void onFieldSizeBarChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            viewModel.setFieldSize(progress + MainViewModel.MIN_GAME_FIELD_SIZE);
        }
    }
}