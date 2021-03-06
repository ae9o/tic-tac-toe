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

package ae9o.tictactoe.gui.utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import java.util.Objects;

/**
 * {@link MutableLiveData} which does not allow the value {@code null} to be stored. Attempting to set/post
 * {@code null} will result in an NullPointerException being thrown.
 *
 * @param <T> the type of data hold by this instance.
 */
public class NonNullMutableLiveData<T> extends MutableLiveData<T> {
    /**
     * Creates a NonNullMutableLiveData initialized with the given {@code non-null} {@code value}.
     *
     * @param value initial non-null value.
     */
    public NonNullMutableLiveData(@NonNull T value) {
        super(Objects.requireNonNull(value));
    }

    @Override
    public void setValue(@NonNull T value) {
        super.setValue(Objects.requireNonNull(value));
    }

    @NonNull
    @Override
    public T getValue() {
        //noinspection ConstantConditions
        return super.getValue();
    }

    @Override
    public void postValue(@NonNull T value) {
        super.postValue(Objects.requireNonNull(value));
    }
}
