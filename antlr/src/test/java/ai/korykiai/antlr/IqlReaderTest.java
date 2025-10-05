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

import ai.koryki.antlr.IQLParser;
import ai.koryki.antlr.Interval;
import ai.koryki.antlr.iql.IQLReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IqlReaderTest {

    @Test
    public void readGrammar() throws IOException {
        String g4 = IQLReader.iqlDefinition();
        assertNotNull(g4);
    }

    @Test
    public void readSucessfull() throws IOException {
        InputStream in = IQLReader.class.getResourceAsStream(
                "/ai/koryki/databases/northwind/iql/entity/entity1.iql");

        IQLReader reader = new IQLReader(in);

        IQLParser.QueryContext query = reader.getQuery();

        assertNotNull(query);
        List<Interval> panic = reader.getPanic();
        assertTrue(panic.isEmpty());
    }
}