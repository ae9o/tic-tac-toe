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

/**
 * Fragment with the game field UI. Passes user input to the game core. Listens to events in the core and renders them.
 */
public class FieldFragment extends Fragment implements TicTacToeGame.OnGameStartListener,
        TicTacToeGame.OnGameFinishListener, TicTacToeGame.OnMarkSetListener, FieldLayout.OnCellClickListener {

    /** Standard colors for drawing marks on the field. */
    private static final int[] DEFAULT_MARK_COLORS = {Color.BLUE, Color.GRAY};

    /** Binding with views from the Fragment's xml resource. */
    private FragmentFieldBinding binding;
    /** Model with current game settings. */
    private MainViewModel viewModel;
    /** The core of the game with the main logic. */
    private TicTacToeGame game;
    /** AI of the opponent. */
    private TicTacToeAi ai;
    /** Generator for random first move. */
    private Random random;

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

        binding.fieldLayout.setOnCellClickListener(this);
        binding.restartButton.setOnClickListener(this::onRestartButtonClick);

        game = new TicTacToeGame();
        game.setOnGameStartListener(this);
        game.setOnMarkSetListener(this);
        game.setOnGameFinishListener(this);

        ai = new TicTacToeAi(game);

        startGame();
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
     * Listens to the {@link TicTacToeGame#setOnGameStartListener(TicTacToeGame.OnGameStartListener)} event.
     *
     * <p>Prepares a field of the appropriate size in the UI.
     *
     * <p>Makes a random move if the AI is chosen as the first player.
     *
     * @param size The size of the field of the started game.
     */
    @Override
    public void onGameStart(int size) {
        binding.fieldLayout.compose(size);

        makeRandomFirstMove();
    }

    /**
     * Listens to the {@link TicTacToeGame#setOnGameFinishListener(TicTacToeGame.OnGameFinishListener)} event.
     *
     * <p>Renders the result of the game in the UI.
     *
     * @param result The result of the game.
     * @param combo The coordinates of the combo collected by the player.
     *              Defined when the {@code result} is {@code TicTacToeGame.GameResult.COMBO}.
     */
    @Override
    public void onGameFinish(TicTacToeGame.GameResult result, TicTacToeGame.Combo combo) {
        showGameResult(result, combo);
    }

    /**
     * Listens to the {@link TicTacToeGame#setOnMarkSetListener(TicTacToeGame.OnMarkSetListener)} event.
     *
     * <p>Renders the set mark in the UI.
     *
     * @param mark The mark set.
     * @param row The row coordinate.
     * @param col The col coordinate.
     */
    @Override
    public void onMarkSet(Mark mark, int row, int col) {
        binding.fieldLayout.getCell(row, col).setMark(mark);
    }

    /**
     * Listens to the {@link FieldLayout#setOnCellClickListener(FieldLayout.OnCellClickListener)} event.
     *
     * <p>Sends information to the core about the setting of the mark by the player.
     *
     * <p>If AI is enabled in the settings, after setting the player's mark, it will automatically set the
     * opponent's mark.
     *
     * @param cell The cell used to set the mark.
     * @param row The row coordinate.
     * @param col The col coordinate.
     */
    @Override
    public void onCellClick(FieldCell cell, int row, int col) {
        if (!game.isActive()) {
            return;
        }
        if (!game.setMark(row, col)) {
            return;
        }
        makeAiMove();
    }

    /**
     * Listens to the {@link View#setOnClickListener(View.OnClickListener)} event of the restart button.
     *
     * <p>Restarts the game when requested by the user.
     *
     * @param button The button clicked.
     */
    private void onRestartButtonClick(View button) {
        startGame();
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
     * Starts a new game.
     *
     * <p>If the previous game is still active, it will automatically finish.
     */
    public void startGame() {
        finishGame();
        clearPreviousGameResult();
        setupMarkColors();

        //noinspection ConstantConditions
        game.start(viewModel.getFieldSize().getValue(), viewModel.getSwapMarks().getValue());
    }

    /**
     * Finishes an active game, if any.
     */
    public void finishGame() {
        if (game.isActive()) {
            game.finish();
        }
    }

    /**
     * Clears previous game results.
     */
    private void clearPreviousGameResult() {
        binding.gameResultText.setVisibility(View.INVISIBLE);
    }

    /**
     * Adjusts the color of player marks depending on the selected game settings. The player's color should be bright
     * and visible, and the AI's should not be flashy.
     */
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
        binding.fieldLayout.setXForegroundColor(DEFAULT_MARK_COLORS[startIndex]);
        binding.fieldLayout.setOForegroundColor(DEFAULT_MARK_COLORS[startIndex ^ 1]);
    }

    /**
     * Places a mark in a random cell in an empty field.
     */
    private void makeRandomFirstMove() {
        //noinspection ConstantConditions
        if (!viewModel.getAiStarts().getValue()) {
            return;
        }
        if (random == null) {
            random = new Random();
        }
        final int size = game.getFieldSize();
        game.setMark(random.nextInt(size), random.nextInt(size));
    }

    /**
     * Sets a mark in the cell suggested by the AI.
     */
    private void makeAiMove() {
        //noinspection ConstantConditions
        if (game.isActive() && viewModel.getAiEnabled().getValue()) {
            TicTacToeAi.Cell guess = ai.guessNextMove();
            game.setMark(guess.getRow(), guess.getCol());
        }
    }

    /**
     * Renders the game result in the UI.
     *
     * @param result The result of the game.
     * @param combo The coordinates of the combo collected by the player.
     *              Defined when the {@code result} is {@code TicTacToeGame.GameResult.COMBO}.
     */
    private void showGameResult(TicTacToeGame.GameResult result, TicTacToeGame.Combo combo) {
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
}