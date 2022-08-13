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
 * Stores coordinates of the winning combo.
 */
public class Combo {
    private int startRow;
    private int startCol;
    private int stopRow;
    private int stopCol;

    void assign(Combo other) {
        startRow = other.startRow;
        startCol = other.startCol;
        stopRow = other.stopRow;
        stopCol = other.stopCol;
    }

    public int getStartRow() {
        return startRow;
    }

    void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    void setStartCol(int startCol) {
        this.startCol = startCol;
    }

    public int getStopRow() {
        return stopRow;
    }

    void setStopRow(int stopRow) {
        this.stopRow = stopRow;
    }

    public int getStopCol() {
        return stopCol;
    }

    void setStopCol(int stopCol) {
        this.stopCol = stopCol;
    }
}
