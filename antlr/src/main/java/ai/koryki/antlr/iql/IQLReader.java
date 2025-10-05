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
package ai.koryki.antlr.iql;

import ai.koryki.antlr.IQLLexer;
import ai.koryki.antlr.IQLParser;
import org.antlr.v4.runtime.*;
import ai.koryki.antlr.AbstractReader;
import ai.koryki.antlr.Interval;
import ai.koryki.antlr.MsgErrorListener;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class IQLReader extends AbstractReader<IQLLexer, IQLParser, IQLParser.QueryContext> {

    private IQLLexer lexer;
    private IQLParser parser;
    private CharStream cs;
    private LineNumberReader lnr;
    private List<Interval> panic;

    private BufferedTokenStream tokens;
    private IQLParser.QueryContext script;
    private MsgErrorListener listener = new MsgErrorListener(false);

    public static String iqlDefinition() {
        return read( "/ai/koryki/antlr/IQL.g4");
    }

    public IQLReader(String sql) throws IOException {

        this(sql, false);
    }

    public IQLReader(String sql, boolean abort) throws IOException {

        this(new StringReader(sql), abort);
    }

    public IQLReader(File in) throws IOException {
        this(in, false);
    }

    public IQLReader(File in, boolean abort) throws IOException {
        this(new FileInputStream(in), StandardCharsets.UTF_8, abort);
    }

    public IQLReader(InputStream in) throws IOException {
        this(in, StandardCharsets.UTF_8, false);
    }

    public IQLReader(InputStream in, boolean abort) throws IOException {
        this(in, StandardCharsets.UTF_8, abort);
    }

    public IQLReader(InputStream in, Charset cs) throws IOException {
        this(in, cs, false);
    }
    public IQLReader(InputStream in, Charset cs, boolean abort) throws IOException {

        this(new InputStreamReader(in, cs), abort);
    }

    public IQLReader(java.io.Reader in) throws IOException {
        this(in, false);
    }
    public IQLReader(java.io.Reader in, boolean abort) throws IOException {
        lnr = new LineNumberReader(in);
        this.cs = CharStreams.fromReader(lnr);
        this.listener.setAbort(abort);
    }

    public IQLReader(CharStream input, boolean abort) {

        cs = input;
        this.listener.setAbort(abort);
    }

    public IQLReader(BufferedTokenStream tokens, List<Interval> panic, IQLParser.QueryContext script) {

        this.tokens = tokens;
        this.panic =  panic;
        this.script = script;
    }

    private long lexduration;
    private long parseduration;

    private void parse() {
        if (script != null) {
            return;
        }
        lex();
        // parsing
        parser = new IQLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        long start = System.currentTimeMillis();
        script = parser.query();
        panic = listener.getPanic();
        parseduration = System.currentTimeMillis() - start;
    }

    private void lex() {
        if (tokens != null) {
            return;
        }
        long start = System.currentTimeMillis();

        lexer = new IQLLexer(cs);
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        tokens = new CommonTokenStream(lexer);
        lexduration = System.currentTimeMillis() - start;
    }

    @Override
    public IQLLexer getLexer() {
        if (tokens == null) {
            lex();
        }
        return lexer;
    }

    public BufferedTokenStream getTokens() {
        if (tokens == null) {
            lex();
        }
        return tokens;
    }

    @Override
    public IQLParser getParser() {
        if (parser == null) {
            parse();
        }
        return parser;
    }

    @Override
    public IQLParser.QueryContext getCtx() {
        return getQuery();
    }

    public IQLParser.QueryContext getQuery() {
        if (script == null) {
            parse();
        }
        return script;
    }

    public String getDescription() {
        return getComment(getQuery());
    }

    public int getLinesOfCode() {

        return lnr != null ? lnr.getLineNumber() : -1;
    }

    public long getLexduration() {
        return lexduration;
    }

    public long getParseduration() {
        return parseduration;
    }

    public long getDuration() {
        return getLexduration() + getParseduration();
    }

    @Override
    public List<Interval> getPanic() {
        return panic;
    }

    public boolean isAbort() {
        return listener.isAbort();
    }

    public void setAbort(boolean abort) {
        listener.setAbort(abort);
    }
}
