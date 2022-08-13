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

package ae9o.tictactoe.game;

/**
 * A snapshot of the game state with the ability to access internal protected methods. AI can safely use this snapshot
 * by arbitrarily changing its internal state, without side effects on the main core of the game.
 */
public class TicTacToeGameSnapshot extends TicTacToeGame {
    @Override
    public void setMarkInternal(int row, int col, Mark mark) {
        super.setMarkInternal(row, col, mark);
    }

    @Override
    public void switchTurn() {
        super.switchTurn();
    }

    @Override
    protected void notifyGameStarted() {
        // Do nothing.
    }

    @Override
    protected void notifyMarkSet(int row, int col) {
        // Do nothing.
    }

    @Override
    protected void notifyGameFinished() {
        // Do nothing.
    }
}
