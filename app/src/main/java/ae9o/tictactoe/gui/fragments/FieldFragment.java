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
import ae9o.tictactoe.game.TicTacToeGame;
import ae9o.tictactoe.game.Combo;
import ae9o.tictactoe.game.GameResult;
import ae9o.tictactoe.game.Mark;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

/**
 * Fragment with the game field UI. Passes user input to the ViewModel. Listens to events in the ViewModel and renders
 * them.
 */
public class FieldFragment extends Fragment {
    /** Default colors for players. */
    private static final int DEFAULT_PLAYER_COLOR = Color.BLUE;
    private static final int DEFAULT_AI_COLOR = Color.GRAY;

    /** Binding with views from the Fragment's xml resource. */
    private FragmentFieldBinding binding;
    /** Model with current game settings. */
    private MainViewModel viewModel;
    /** Colors for drawing marks on the field. */
    private final int[] markColors = new int[Mark.values().length];

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
        binding = FragmentFieldBinding.inflate(inflater, container, false);
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
        viewModel.getXScore().observe(lifecycleOwner, this::onXScoreChanged);
        viewModel.getOScore().observe(lifecycleOwner, this::onOScoreChanged);

        viewModel.setOnGameStartListener(this::onGameStart);
        viewModel.setOnMarkSetListener(this::onMarkSet);
        viewModel.setOnGameFinishListener(this::onGameFinish);

        binding.fieldLayout.setOnCellClickListener(this::onCellClick);
        binding.restartButton.setOnClickListener(this::onRestartButtonClick);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        viewModel.replay();
    }

    /**
     * Disposes fragment's resources.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        viewModel.setOnGameStartListener(null);
        viewModel.setOnMarkSetListener(null);
        viewModel.setOnGameFinishListener(null);

        binding = null;
    }

    /**
     * Listens to the {@link MainViewModel#setOnGameStartListener(TicTacToeGame.OnGameStartListener)} event.
     *
     * <p>Prepares the UI of a field of the appropriate size.
     *
     * @param fieldSize The size of the field in the started game.
     */
    private void onGameStart(int fieldSize) {
        clearGameResult();
        setupMarkColors();
        binding.fieldLayout.compose(fieldSize);
    }

    /**
     * Listens to the {@link MainViewModel#setOnGameFinishListener(TicTacToeGame.OnGameFinishListener)} event.
     *
     * <p>Renders the result of the game.
     *
     * @param result The result of the game.
     * @param combo The coordinates of the combo collected by the player.
     *              Defined when the {@code result} is {@code TicTacToeGame.GameResult.COMBO}.
     */
    private void onGameFinish(GameResult result, Combo combo) {
        showGameResult(result, combo);
    }

    /**
     * Listens to the {@link MainViewModel#setOnMarkSetListener(TicTacToeGame.OnMarkSetListener)} event.
     *
     * <p>Renders the set mark.
     *
     * @param mark The mark set.
     * @param row The row coordinate.
     * @param col The col coordinate.
     */
    private void onMarkSet(Mark mark, int row, int col) {
        final FieldCell cell = binding.fieldLayout.getCell(row, col);
        cell.setForegroundColor(getMarkColor(mark));
        cell.setMark(mark);
    }

    /**
     * Listens to the {@link FieldLayout#setOnCellClickListener(FieldLayout.OnCellClickListener)} event.
     *
     * <p>Sends information to the core about the mark to set.
     *
     * @param cell The clicked cell.
     * @param row The row coordinate.
     * @param col The col coordinate.
     */
    private void onCellClick(FieldCell cell, int row, int col) {
        viewModel.setMark(row, col, true);
    }

    /**
     * Listens to the {@link View#setOnClickListener(View.OnClickListener)} event of the restart button.
     *
     * <p>Restarts the game when requested by a user.
     *
     * @param button The button clicked.
     */
    private void onRestartButtonClick(View button) {
        viewModel.startGame();
    }

    /**
     * Observes the {@link MainViewModel#getXScore()} setting and renders it in the UI.
     *
     * @param xScore Current value.
     */
    private void onXScoreChanged(Integer xScore) {
        binding.xScoreText.setText(getString(R.string.field_x_score_text, xScore));
        binding.xMark.startAnimation();
    }

    /**
     * Observes the {@link MainViewModel#getOScore()} setting and renders it in the UI.
     *
     * @param oScore Current value.
     */
    private void onOScoreChanged(Integer oScore) {
        binding.oScoreText.setText(getString(R.string.field_o_score_text, oScore));
        binding.oMark.startAnimation();
    }

    /**
     * Adjusts the color of marks depending on the selected game settings.
     */
    private void setupMarkColors() {
        final int first = viewModel.getCurrentTurn().ordinal();
        final int second = viewModel.getNextTurn().ordinal();
        if (viewModel.isAiStarts()) {
            markColors[first] = DEFAULT_AI_COLOR;
            markColors[second] = DEFAULT_PLAYER_COLOR;
        } else {
            markColors[first] = DEFAULT_PLAYER_COLOR;
            markColors[second] = DEFAULT_AI_COLOR;
        }
    }

    /**
     * Returns the customized color for the given mark.
     *
     * @param mark The mark to be drawn.
     * @return The color for the mark.
     */
    private int getMarkColor(Mark mark) {
        return markColors[mark.ordinal()];
    }

    /**
     * Renders the game result.
     *
     * @param result The result of the game.
     * @param combo The coordinates of the combo collected by the player.
     *              Defined when the {@code result} is {@code TicTacToeGame.GameResult.COMBO}.
     */
    private void showGameResult(GameResult result, Combo combo) {
        switch (result) {
            case COMBO:
                binding.fieldLayout.setCombo(combo);
                break;

            case DRAW:
                binding.gameResultText.setVisibility(View.VISIBLE);
                binding.xMark.startAnimation();
                binding.oMark.startAnimation();
                break;

            default:
                clearGameResult();
                break;
        }
    }

    private void clearGameResult() {
        binding.gameResultText.setVisibility(View.INVISIBLE);
    }
}