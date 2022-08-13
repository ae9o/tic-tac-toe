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
 * IDs for marks placed by players in the cells of the field.
 */
public enum Mark {
    EMPTY,
    X,
    O;

    /**
     * Returns the mark corresponding to the numeric code.
     *
     * @param ordinal The numeric code to be converted.
     * @return The corresponding mark.
     */
    public static Mark valueOf(int ordinal) {
        final Mark[] values = values();
        if ((ordinal < 0) || (ordinal >= values.length)) {
            throw new IllegalArgumentException("There is no mark with such ordinal: " + ordinal);
        }
        return values[ordinal];
    }
}
