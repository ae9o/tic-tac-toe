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

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private MainViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    public void onStopTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        onFieldSizeBarChanged(seekBar, progress, fromUser);
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void onAiEnabledChanged(boolean aiEnabled) {
        binding.aiEnabledSwitch.setChecked(aiEnabled);
    }

    private void onAiEnabledSwitchChanged(View button, boolean isChecked) {
        viewModel.setAiEnabled(isChecked);
    }

    private void onAiStartsChanged(boolean aiStarts) {
        binding.aiStartsSwitch.setChecked(aiStarts);
    }

    private void onAiStartsSwitchChanged(View button, boolean isChecked) {
        viewModel.setAiStarts(isChecked);
    }

    private void onSwapMarksChanged(boolean swapMarks) {
        binding.swapMarksSwitch.setChecked(swapMarks);
    }

    private void onSwapMarksSwitchChanged(View button, boolean isChecked) {
        viewModel.setSwapMarks(isChecked);
    }

    private void onFieldSizeBarChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            viewModel.setFieldSize(progress + MainViewModel.MIN_GAME_FIELD_SIZE);
        }
    }

    private void onFieldSizeChanged(int size) {
        binding.fieldSizeText.setText(getString(R.string.settings_field_size_text, size));
        binding.fieldSizeBar.setProgress(size - MainViewModel.MIN_GAME_FIELD_SIZE);
    }
}