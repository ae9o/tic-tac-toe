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

package ae9o.tictactoe.gui;

import ae9o.tictactoe.gui.utils.NonNullMutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    public static final int MIN_GAME_FIELD_SIZE = 3;
    public static final int MAX_GAME_FIELD_SIZE = 15;
    public static final int DEFAULT_GAME_FIELD_SIZE = 3;

    private final NonNullMutableLiveData<Integer> fieldSize = new NonNullMutableLiveData<>(DEFAULT_GAME_FIELD_SIZE);
    private final NonNullMutableLiveData<Boolean> aiEnabled = new NonNullMutableLiveData<>(true);
    private final NonNullMutableLiveData<Boolean> aiStarts = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Boolean> swapMarks = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Integer> xScore = new NonNullMutableLiveData<>(0);
    private final NonNullMutableLiveData<Integer> oScore = new NonNullMutableLiveData<>(0);

    public LiveData<Integer> getFieldSize() {
        return fieldSize;
    }

    public void setFieldSize(int fieldSize) {
        this.fieldSize.setValue(fieldSize);
    }

    public LiveData<Boolean> getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled.setValue(aiEnabled);
    }

    public LiveData<Boolean> getAiStarts() {
        return aiStarts;
    }

    public void setAiStarts(boolean aiStarts) {
        this.aiStarts.setValue(aiStarts);
    }

    public LiveData<Boolean> getSwapMarks() {
        return swapMarks;
    }

    public void setSwapMarks(boolean swapMarks) {
        this.swapMarks.setValue(swapMarks);
    }

    public LiveData<Integer> getXScore() {
        return xScore;
    }

    public void setXScore(int xScore) {
        this.xScore.setValue(xScore);
    }

    public LiveData<Integer> getOScore() {
        return oScore;
    }

    public void setOScore(int oScore) {
        this.oScore.setValue(oScore);
    }

    public void clearScore() {
        xScore.setValue(0);
        oScore.setValue(0);
    }
}
