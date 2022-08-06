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

package ae9o.tictactoe.core;

/**
 * An interface to be implemented by the AIs intended for this game.
 */
public interface TicTacToeAi {
    /** The limit on the algorithm running time in nanoseconds (default is 1 second). */
    long MAX_SEARCH_TIME = 1000000000L;

    /**
     * Returns the best move according to this AI for the given game state.
     *
     * @param snapshot A fixed snapshot of the game in a particular state. This snapshot must not be changed by external
     *                 code during AI work. Some AI implementations may process the snapshot on other threads.
     * @return The best move according to this AI for the given game state.
     */
    Cell guessNextMove(TicTacToeGame snapshot);

    /**
     * Stores coordinates of a cell.
     */
    class Cell {
        private int row;
        private int col;

        public void set(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public void set(Cell other) {
            row = other.getRow();
            col = other.getCol();
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }
    }
}
