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

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public abstract class AbstractReader<L extends Lexer, P extends Parser, C extends ParseTree> {


    public abstract L getLexer() ;
    public abstract P getParser();
    public abstract BufferedTokenStream getTokens();
    public abstract List<Interval> getPanic();
    public abstract C getCtx();

    public int getTokenCount() {
        return getTokens() != null ? getTokens().size() : -1;
    }

    public String getComment(ParseTree node) {
        List<Token> hiddenTokens =
                getTokens().getHiddenTokensToLeft(node.getSourceInterval().a);

        StringBuilder b = new StringBuilder();
        if (hiddenTokens != null) {

            for (Token t : hiddenTokens) {

                String c = trimComment(t.getText());
                if (c.isEmpty()) {
                    continue;
                }
                if (b.length() > 0) {
                    b.append(System.lineSeparator());
                }
                b.append(c);
            }
        }
        return b.length() > 0 ? b.toString() : null;
    }

    private String trimComment(String c) {

        String t = c.trim();

        if (c.startsWith("/*") && c.endsWith("*/")) {
            c = c.substring(0, c.length() - 4).trim();
            return c.substring(2, c.length());
        } else if (c.startsWith("//")) {
            c = c.trim();
            return c.substring(2, c.length());
        } else if (c.trim().isEmpty()) {
            return "";
        } else {
            return c.trim();
        }
    }

}

