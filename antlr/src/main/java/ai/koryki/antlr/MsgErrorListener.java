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

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MsgErrorListener extends BaseErrorListener {

    private boolean abort;
    private List<Interval> panic = new ArrayList<>();

    public MsgErrorListener(boolean abort) {
        this.abort = abort;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e)
    {

        if (recognizer instanceof Parser) {

            if (abort) {
                abort((Parser) recognizer, offendingSymbol, line, charPositionInLine, msg);
            } else {
                panic((Parser) recognizer, (Token) offendingSymbol, msg);
            }
        } else if (recognizer instanceof Lexer) {
            String l = "line " + line + ":" + charPositionInLine + " at " + offendingSymbol + " : " + msg;

            GrammarException se = new GrammarException("Lexer" + System.lineSeparator() + l);
            se.setLine(line);
            se.setPos(charPositionInLine);
            throw se;
        } else if (recognizer == null) {
            GrammarException se = new GrammarException("null" + System.lineSeparator());
            se.setLine(line);
            se.setPos(charPositionInLine);
            throw se;
        } else {
            GrammarException se = new GrammarException(recognizer.getClass().getSimpleName() + "1 " + System.lineSeparator() + msg);
            se.setLine(line);
            se.setPos(charPositionInLine);
            throw se;
        }
    }

    private static void abort(Parser recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg) {
        List<String> stack = recognizer.getRuleInvocationStack();
        Collections.reverse(stack);

        String s = "rule stack: " + stack;
        String l = "line " + line + ":" + charPositionInLine + " at " + offendingSymbol + " : " + msg;

        GrammarException se = new GrammarException(s + System.lineSeparator() + l);
        se.setLine(line);
        se.setPos(charPositionInLine);
        throw se;
    }

    private void panic(Parser recognizer, Token offendingSymbol, String msg) {
        Parser parser = recognizer;
        Token offendingToken = offendingSymbol;

        ParserRuleContext context = parser.getContext();
        int startIndex = least(context);
        int stopIndex = offendingToken.getTokenIndex();

        if (startIndex < stopIndex) {
            panic.add(new Interval(startIndex + 1, stopIndex, msg));
        }
    }

    private static int least(ParseTree context) {

        if (context instanceof ParserRuleContext) {
            ParserRuleContext c = (ParserRuleContext)context;
            if (c.children != null && !c.children.isEmpty()) {
                return least(c.children.get(c.children.size() - 1));
            } else if (c.stop != null) {
                return c.stop.getTokenIndex();
            } else {
                return c.start.getTokenIndex();
            }
        } else if (context instanceof TerminalNode) {
            TerminalNode n = (TerminalNode)context;
            return n.getSymbol().getTokenIndex();
        } else  {
            throw new RuntimeException();
        }
    }

    public List<Interval> getPanic() {
        return panic;
    }

    public boolean isAbort() {
        return abort;
    }

    public void setAbort(boolean abort) {
        this.abort = abort;
    }
}
