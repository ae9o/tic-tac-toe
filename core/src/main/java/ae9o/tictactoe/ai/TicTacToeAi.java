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

package ae9o.tictactoe.ai;

import ae9o.tictactoe.game.TicTacToeGameSnapshot;

/**
 * An interface to be implemented by AIs intended for this game.
 */
public interface TicTacToeAi {
    /** Recommended limit for an algorithm running time in milliseconds (1 second). */
    long MAX_SEARCH_TIME = 1000L;

    /**
     * Returns the best move for the given game state according to this AI implementation.
     *
     * @param snapshot A fixed snapshot of the game in a particular state. This snapshot is not modified by external
     *                 code while the AI is running. Some AI implementations may process the snapshot on other threads.
     * @return The best move according to this AI implementation.
     */
    TicTacToeAiResult guessNextMove(TicTacToeGameSnapshot snapshot);
}
