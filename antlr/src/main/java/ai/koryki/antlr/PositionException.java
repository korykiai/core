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
package ai.koryki.antlr;

public class PositionException extends RuntimeException {

    private int line;
    private int pos;

    public PositionException() {
        super();
    }

    public PositionException(String msg) {

        super(msg);
    }

    public PositionException(Throwable cause, int line, int pos) {

        super(cause);
        this.line = line;
        this.pos = pos;
    }

    public PositionException(Throwable cause) {

        super(cause);
    }

    public PositionException(String msg, Throwable cause) {

        super(msg, cause);
    }

    public int getLine() {

        return line;
    }

    public void setLine(int line) {

        this.line = line;
    }

    public int getPos() {

        return pos;
    }

    public void setPos(int pos) {

        this.pos = pos;
    }

    @Override
    public String toString() {
        return super.toString() + " " + line + "/" + pos;
    }
}
