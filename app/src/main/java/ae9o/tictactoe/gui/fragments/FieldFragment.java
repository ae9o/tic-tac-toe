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
import ae9o.tictactoe.core.TicTacToeAi;
import ae9o.tictactoe.core.TicTacToeGame;
import ae9o.tictactoe.core.TicTacToeGame.Mark;
import ae9o.tictactoe.databinding.FragmentFieldBinding;
import ae9o.tictactoe.gui.MainViewModel;
import ae9o.tictactoe.gui.views.FieldCell;
import ae9o.tictactoe.gui.views.FieldLayout;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import java.util.Random;

public class FieldFragment extends Fragment implements TicTacToeGame.OnGameStartListener,
        TicTacToeGame.OnGameFinishListener, TicTacToeGame.OnMarkSetListener, FieldLayout.OnCellClickListener {

    private static final int[] MARK_COLORS = {Color.BLUE, Color.GRAY};

    private FragmentFieldBinding binding;
    private MainViewModel viewModel;
    private TicTacToeGame game;
    private TicTacToeAi ai;
    private Random random;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFieldBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LifecycleOwner lifecycleOwner = getViewLifecycleOwner();

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getXScore().observe(lifecycleOwner, this::onXScoreChanged);
        viewModel.getOScore().observe(lifecycleOwner, this::onOScoreChanged);

        binding.fieldLayout.setOnCellClickListener(this);
        binding.restartButton.setOnClickListener(this::onRestartButtonClick);

        game = new TicTacToeGame();
        game.setOnGameStartListener(this);
        game.setOnMarkSetListener(this);
        game.setOnGameFinishListener(this);

        ai = new TicTacToeAi(game);

        restart();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onGameStart(int size) {
        binding.fieldLayout.compose(size);
    }

    @Override
    public void onGameFinish(TicTacToeGame.GameResult result, TicTacToeGame.Combo combo) {
        switch (result) {
            case COMBO:
                binding.fieldLayout.setCombo(combo);

                switch (game.getMark(combo.getStartRow(), combo.getStartCol())) {
                    case X:
                        //noinspection ConstantConditions
                        viewModel.setXScore(viewModel.getXScore().getValue() + 1);
                        break;

                    case O:
                        //noinspection ConstantConditions
                        viewModel.setOScore(viewModel.getOScore().getValue() + 1);
                        break;
                }
                break;

            case DRAW:
                binding.gameResultText.setVisibility(View.VISIBLE);
                binding.xMark.startAnimation();
                binding.oMark.startAnimation();
                break;
        }
    }

    @Override
    public void onMarkSet(Mark mark, int row, int col) {
        binding.fieldLayout.getCell(row, col).setMark(mark);
    }

    @Override
    public void onCellClick(FieldCell cell, int row, int col) {
        if (!game.isActive()) {
            return;
        }
        if (!game.setMark(row, col)) {
            return;
        }
        //noinspection ConstantConditions
        if (game.isActive() && viewModel.getAiEnabled().getValue()) {
            TicTacToeAi.Cell guess = ai.guessNextMove();
            game.setMark(guess.getRow(), guess.getCol());
        }
    }

    private void onRestartButtonClick(View button) {
        restart();
    }

    private void onXScoreChanged(Integer xScore) {
        binding.xScoreText.setText(getString(R.string.field_x_score_text, xScore));
        binding.xMark.startAnimation();
    }

    private void onOScoreChanged(Integer oScore) {
        binding.oScoreText.setText(getString(R.string.field_o_score_text, oScore));
        binding.oMark.startAnimation();
    }

    public void restart() {
        if (game.isActive()) {
            game.finish();
        }
        binding.gameResultText.setVisibility(View.INVISIBLE);

        setupMarkColors();
        //noinspection ConstantConditions
        game.start(viewModel.getFieldSize().getValue(), viewModel.getSwapMarks().getValue());
        makeFirstMove();
    }

    private void setupMarkColors() {
        int startIndex = 0;
        //noinspection ConstantConditions
        if (viewModel.getAiStarts().getValue()) {
            startIndex ^= 1;
        }
        //noinspection ConstantConditions
        if (viewModel.getSwapMarks().getValue()) {
            startIndex ^= 1;
        }
        binding.fieldLayout.setXForegroundColor(MARK_COLORS[startIndex]);
        binding.fieldLayout.setOForegroundColor(MARK_COLORS[startIndex ^ 1]);
    }

    private void makeFirstMove() {
        //noinspection ConstantConditions
        if (!viewModel.getAiStarts().getValue()) {
            return;
        }
        if (random == null) {
            random = new Random();
        }
        final int size = game.getSize();
        game.setMark(random.nextInt(size), random.nextInt(size));
    }
}