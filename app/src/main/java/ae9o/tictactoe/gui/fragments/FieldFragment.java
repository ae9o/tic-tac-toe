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
import ae9o.tictactoe.core.TicTacToeGame.Combo;
import ae9o.tictactoe.core.TicTacToeGame.GameResult;
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
public class FieldFragment extends Fragment {
    /** Default colors for players. */
    private static final int DEFAULT_PLAYER_COLOR = Color.BLUE;
    private static final int DEFAULT_AI_COLOR = Color.GRAY;

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

        binding.fieldLayout.setOnCellClickListener(this::onCellClick);
        binding.restartButton.setOnClickListener(this::onRestartButtonClick);

        game = new TicTacToeGame();
        game.setOnGameStartListener(this::onGameStart);
        game.setOnMarkSetListener(this::onMarkSet);
        game.setOnGameFinishListener(this::onGameFinish);

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
     * <p>Prepares the UI of a field of the appropriate size.
     *
     * <p>Makes a random move if the AI is chosen as the first player.
     *
     * @param size The size of the field in the started game.
     */
    private void onGameStart(int size) {
        binding.fieldLayout.compose(size);
        setupMarkColors();
        makeRandomFirstMove();
    }

    /**
     * Listens to the {@link TicTacToeGame#setOnGameFinishListener(TicTacToeGame.OnGameFinishListener)} event.
     *
     * <p>Updates the score.
     *
     * <p>Renders the result of the game.
     *
     * @param result The result of the game.
     * @param combo The coordinates of the combo collected by the player.
     *              Defined when the {@code result} is {@code TicTacToeGame.GameResult.COMBO}.
     */
    private void onGameFinish(GameResult result, Combo combo) {
        updateScore(result, combo);
        showGameResult(result, combo);
    }

    /**
     * Listens to the {@link TicTacToeGame#setOnMarkSetListener(TicTacToeGame.OnMarkSetListener)} event.
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
     * <p>If AI is enabled in the settings, it will automatically set an opponent's mark
     * after setting the player's mark
     *
     * @param cell The clicked cell.
     * @param row The row coordinate.
     * @param col The col coordinate.
     */
    private void onCellClick(FieldCell cell, int row, int col) {
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
     * <p>Restarts the game when requested by a user.
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
        clearGameResult();

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
     * Adjusts the color of marks depending on the selected game settings.
     */
    private void setupMarkColors() {
        final int first = game.getCurrentTurn().ordinal();
        final int second = game.getNextTurn().ordinal();
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
     * Places a mark in a random cell on an empty field.
     */
    private void makeRandomFirstMove() {
        if (!viewModel.isAiStarts()) {
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
            final TicTacToeAi.Cell guess = ai.guessNextMove();
            game.setMark(guess.getRow(), guess.getCol());
        }
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
        }
    }

    /**
     * Clears previous game results.
     */
    private void clearGameResult() {
        binding.gameResultText.setVisibility(View.INVISIBLE);
    }

    /**
     * Adds points to the winner.
     *
     * @param result The result of the game.
     * @param combo The coordinates of the combo collected by the player.
     *              Defined when the {@code result} is {@code TicTacToeGame.GameResult.COMBO}.
     */
    private void updateScore(GameResult result, Combo combo) {
        if (result != GameResult.COMBO) {
            return;
        }
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
    }
}