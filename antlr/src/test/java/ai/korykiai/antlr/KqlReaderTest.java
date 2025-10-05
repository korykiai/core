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
package ai.korykiai.antlr;

import ai.koryki.antlr.GrammarException;
import ai.koryki.antlr.Interval;
import ai.koryki.antlr.KQLParser;
import ai.koryki.antlr.kql.KQLReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KqlReaderTest {

    @Test
    public void readGrammar() throws IOException {
        String g4 = KQLReader.kqlDefinition();
    }

    @Test
    public void readSucessfull() throws IOException {
        InputStream in = KQLReader.class.getResourceAsStream(
                "/ai/koryki/databases/northwind/demo/employeeranking.kql");

        KQLReader reader = new KQLReader(in);

        KQLParser.QueryContext query = reader.getQuery();

        assertNotNull(query);
        List<Interval> panic = reader.getPanic();
        assertTrue(panic.isEmpty());
    }

    @Test
    public void readAbort() throws IOException {
        InputStream in = KQLReader.class.getResourceAsStream(
                "/ai/koryki/antlr/employeeranking_panic.lql");

        try {
            KQLReader reader = new KQLReader(in, true);
            reader.getQuery();
            fail("expected exception not thrown");
        } catch (GrammarException ignored) {
            // empty
        }
    }

    @Test
    public void readPanic() throws IOException {
        InputStream in = KQLReader.class.getResourceAsStream(
                "/ai/koryki/antlr/employeeranking_panic.lql");

        KQLReader reader = new KQLReader(in, false);
        KQLParser.QueryContext query = reader.getQuery();
        assertNotNull(query);
        List<Interval> panic = reader.getPanic();
        assertFalse(panic.isEmpty());
    }
}