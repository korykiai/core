/*
 * Copyright 2025 Johannes Zemlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package ai.koryki.iql.rules;

import java.util.Arrays;
import java.util.Optional;

public enum Function {
    add(false, "+"),
    minus(false, "-"),
    multiply(false, "*"),
    divide(false, "/"),
    count(true),
    sum(true),
    avg(true),
    min(true),
    max(true);

    private boolean aggregat;
    private String operator;

    Function(boolean aggregat) {
        this.aggregat = aggregat;
    }

    Function(boolean aggregat, String operator) {
        this.aggregat = aggregat;
        this.operator = operator;
    }

    public static Optional<Function> fromString(String name) {
        return Arrays.stream(Function.values())
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public boolean isAggregat() {
        return aggregat;
    }

    public String getOperator() {
        return operator;
    }
}
