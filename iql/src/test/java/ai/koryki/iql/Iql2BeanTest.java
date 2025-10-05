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
package ai.koryki.iql;

import ai.koryki.antlr.iql.IQLReader;
import ai.koryki.iql.query.Query;
import ai.koryki.model.schema.Schema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Iql2BeanTest {

    private static Schema northwind;
    private static RelationResolver resolver;

    @BeforeAll
    public static void readNorthwindDB() throws IOException {

        northwind = readSchemaFromResource("/ai/koryki/databases/northwind/schema.json");
        Map<String, List<String>> links = readHashSetFromResource("/ai/koryki/databases/northwind/links.json");
        resolver = new RelationResolver(northwind, links);
    }

    @Test
    public void onedown() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/link/onedown.iql";

        String expected = "link/onedown.sql";
        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void outerjoinandfilter() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/link/outerjoinandfilter.iql";

        String expected = "link/outerjoinandfilter.sql";
        String s = test(expected, q);
        System.out.println(s);
    }


    @Test
    public void towdown() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/link/twodown.iql";
        String expected = "link/twodown.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void fourjoin() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/link/fourjoin.iql";

        String expected = "link/fourjoin.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void cte1() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/cte/cte1.iql";

        String expected = "cte/cte1.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void expression1() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/expression/expression1.iql";

        String expected = "expression/expression1.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void expression2() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/expression/expression2.iql";

        String expected = "expression/expression2.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void unsold_products1() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/expression/unsold_products1.iql";

        String expected = "expression/unsold_products1.sql";
        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void cte2() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/cte/cte2.iql";

        String expected = "cte/cte2.sql";

        String s = test(expected, q);
        System.out.println(s);

    }

    @Test
    public void entity1() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/entity/entity1.iql";
        String expected = "entity/entity1.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void entity2() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/entity/entity2.iql";
        String expected = "entity/entity2.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void entity3() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/entity/entity3.iql";
        String expected = "entity/entity3.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void entity4() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/entity/entity4.iql";
        String expected = "entity/entity4.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    @Test
    public void complex1() throws IOException {

        String q = "/ai/koryki/databases/northwind/iql/complex/complex1.iql";
        String expected = "complex/complex1.sql";

        String s = test(expected, q);
        System.out.println(s);
    }

    private static String test(String expected, String in) throws IOException {

        String sqldir = "/ai/koryki/databases/northwind/sql/";
        String jdbcdir = "/ai/koryki/databases/northwind/jdbc/";

        String kql = convert(Iql2Bean.class.getResourceAsStream(in));
        Query bean = new Iql2Bean(new IQLReader(kql, true)).toScript();
        String sql = new Bean2Sql(resolver, bean).toSql();
        FileAsserter.resourceAssert(sqldir + expected, sql);

        String jdbc = new Bean2Jdbc(resolver, bean).toSql();

        FileAsserter.resourceAssert(jdbcdir + expected, jdbc);

        String bean2kql = new Bean2Iql(bean).toString();
        Query bean2 = new Iql2Bean(new IQLReader(bean2kql, true)).toScript();
        String sql2 = new Bean2Sql(resolver, bean2).toSql();

        FileAsserter.scriptAssert(sql, sql2);

        return sql;
    }

    public static String convert(InputStream in) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = in.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

    public static Schema readSchemaFromResource(String ressource) throws IOException {

        InputStream i = Iql2Bean.class.getResourceAsStream(ressource);
        return readDatabaseJson(i);
    }
    public static Schema readDatabaseJson(InputStream in) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return mapper.readValue(r, Schema.class );
        }
    }

    public static HashMap readHashSetFromResource(String ressource) throws IOException {
        InputStream i = Iql2Bean.class.getResourceAsStream(ressource);
        return readHashSetFromJson(i);
    }

    public static HashMap<String, List<String>> readHashSetFromJson(InputStream in) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {

            return  mapper.readValue(r, new TypeReference<HashMap<String, List<String>>>() {});
        }
    }
}
