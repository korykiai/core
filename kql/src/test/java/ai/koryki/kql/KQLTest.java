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
package ai.koryki.kql;

import ai.koryki.antlr.KQLParser;
import ai.koryki.antlr.kql.KQLReader;
import ai.koryki.iql.Bean2Sql;
import ai.koryki.iql.RelationResolver;
import ai.koryki.iql.query.Query;
import ai.koryki.model.JsonUtil;
import ai.koryki.model.schema.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KQLTest {

    private static Schema northwind;
    private static RelationResolver resolver;

    @BeforeAll
    public static void readNorthwindDB() throws IOException {

        northwind = JsonUtil.readSchemaFromResource("/ai/koryki/databases/northwind/schema.json");
        Map<String, List<String>> links = JsonUtil.readHashSetFromResource("/ai/koryki/databases/northwind/links.json");

        resolver = new RelationResolver(northwind, links);
    }

    @Test
    public void customerwithordersin2023() throws IOException {

        InputStream q = KQLTest.class.getResourceAsStream("/ai/koryki/databases/northwind/demo/customerswithordersin2023.kql");

        String sql = toSql(q);
        assertNotNull(sql);
    }

    @Test
    public void customersmorethan10ordersin2023() throws IOException {

        InputStream q = KQLTest.class.getResourceAsStream("/ai/koryki/databases/northwind/demo/customersmorethan10ordersin2023.kql");

        String sql = toSql(q);
        assertNotNull(sql);
    }

    @Test
    public void employeeranking() throws IOException {

        InputStream q = KQLTest.class.getResourceAsStream("/ai/koryki/databases/northwind/demo/employeeranking.kql");

        String sql = toSql(q);
        assertNotNull(sql);
    }


    @Test
    public void customersingermany() throws IOException {

        InputStream q = KQLTest.class.getResourceAsStream("/ai/koryki/databases/northwind/demo/customersingermany.kql");

        String sql = toSql(q);
    }

    @Test
    public void complex1() throws IOException {

        InputStream q = KQLTest.class.getResourceAsStream("/ai/koryki/databases/northwind/kql/complex/complex1.kql");

        String sql = toSql(q);
    }

    @Test
    public void suppliersandproductsincategory() throws IOException {

        InputStream q = KQLTest.class.getResourceAsStream("/ai/koryki/databases/northwind/demo/suppliersandproductsincategory.kql");

        String sql = toSql(q);
        System.out.println(sql);
    }

    @Test
    public void customersmorethan10orders() throws IOException {

        resolver.setStrict(true);
        InputStream q = KQLTest.class.getResourceAsStream("/ai/koryki/databases/northwind/kql/customersmorethan10orders.kql");

        String sql = toSql(q);
        System.out.println(sql);
        resolver.setStrict(false);
    }

    private String toSql(InputStream lql) throws IOException {

        KQLReader r = new KQLReader(lql);

        KQLParser.QueryContext script = r.getCtx();

        KQL2Bean l = new KQL2Bean(script, r.getDescription());
        Query s = l.toBean();
        Bean2Sql k = new Bean2Sql(resolver, s);

        formatter(script);

        return k.toEnhancedSql();
    }

    private void formatter(KQLParser.QueryContext script) throws IOException {

        Query s = new KQL2Bean(script, null).toBean();
        String sql1 = new Bean2Sql(resolver, s).toEnhancedSql();

        String format = new KQLFormatter(script, null).format();

        KQLParser.QueryContext script2 = new KQLReader(format).getCtx();
        Query s2 = new KQL2Bean(script2, null).toBean();
        String sql2 = new Bean2Sql(resolver, s2).toEnhancedSql();

        assertEquals(sql1, sql2);
    }
//    public static String convert(InputStream in) throws IOException {
//        ByteArrayOutputStream result = new ByteArrayOutputStream();
//        byte[] buffer = new byte[8192];
//        int length;
//        while ((length = in.read(buffer)) != -1) {
//            result.write(buffer, 0, length);
//        }
//        return result.toString(StandardCharsets.UTF_8.name());
//    }
//
//    public static Schema readSchemaFromResource(String ressource) throws IOException {
//
//        InputStream i = Iql2Bean.class.getResourceAsStream(ressource);
//        return readDatabaseJson(i);
//    }
//    public static Schema readDatabaseJson(InputStream in) throws IOException {
//
//        ObjectMapper mapper = new ObjectMapper();
//        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
//            return mapper.readValue(r, Schema.class );
//        }
//    }
//
//    public static HashMap readHashSetFromResource(String ressource) throws IOException {
//        InputStream i = Iql2Bean.class.getResourceAsStream(ressource);
//        return readHashSetFromJson(i);
//    }
//
//    public static HashMap<String, List<String>> readHashSetFromJson(InputStream in) throws IOException {
//
//        ObjectMapper mapper = new ObjectMapper();
//        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
//
//            return  mapper.readValue(r, new TypeReference<HashMap<String, List<String>>>() {});
//        }
//    }
}
