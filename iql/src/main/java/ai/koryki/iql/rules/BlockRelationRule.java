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

import ai.koryki.iql.Identifier;
import ai.koryki.iql.RelationResolver;
import ai.koryki.iql.query.*;
import ai.koryki.model.schema.Relation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Add OUT-Expressions for Join-Columns, references by other queries.
 */
public class BlockRelationRule {

    private RelationResolver resolver;
    private Query query;
    private Map<String, Table> idToTableMap;


    public BlockRelationRule(Query query, RelationResolver resolver, Map<String, Table> idToTableMap) {
        this.query = query;
        this.resolver = resolver;
        this.idToTableMap = idToTableMap;
    }

    public void apply() {
        apply(query);
    }

    private void apply(Query query) {

        query.getBlock().forEach(b -> apply(b.getSet()));
        apply(query.getSet());
    }

    private void apply(Set set) {
        if (set.getOperator() != null) {
            apply(set.getLeft());
            apply(set.getRight());
        } else {
            apply(set.getSelect());
        }
    }
    private void apply(Select select) {

        apply(select.getStart(), select.getJoin());
    }

    protected void apply(Table left, List<Join> join) {

            for (Join j : join) {

                joinColumns(left, j, 0);
                apply(j.getTable(), j.getJoin());
            }
    }

    private void joinColumns(Table left, Join join, int indent) {

        String msg = left.getName() + (left.getAlias() != null ? " " + left.getAlias() : "");
        String crit = join.getCrit();
        Table right = join.getTable();

        Table start = join.isInvers() ? right : left;
        Table end = join.isInvers() ? left : right;

         joinColumns(indent, start, end, crit, msg, right);
    }


    private void joinColumns(int indent, Table start, Table end, String crit, String msg, Table right) {
        String startTable = start.getName();
        String endTable = end.getName();

        boolean b1 = resolver.isTableInDatabase(startTable);
        Table s = b1 ? start : idToTableMap.get(startTable);
        startTable = s.getName();
        startTable = Identifier.normal(Identifier.lowercase, startTable);

        boolean b2 = resolver.isTableInDatabase(endTable);
        Table e = b2 ? end : idToTableMap.get(endTable);
        endTable = e.getName();


        endTable = Identifier.normal(Identifier.lowercase, endTable);

        Optional<Relation> o = resolver.find(startTable, endTable, crit);

        if (!o.isPresent()) {
            throw new RuntimeException(msg +  " " + crit + " " + right.getName());
        }
        Relation r = o.get();

        if (!b1) {
            List<String> cols = r.getStartColumns();
            enhanceOut(s, cols);
        }
        if (!b2) {
            List<String> cols = r.getEndColumns();
            enhanceOut(e, cols);
        }
    }

    private void enhanceOut(Table table, List<String> cols) {
        for (String c : cols) {
            if (!hasOut(table, c)) {
                Out out = createOut(table, c);
                table.getOut().add(out);
            }
        }
    }

    private static Out createOut(Table table, String c) {
        Out out = new Out();
        Column col = new Column();
        col.setAlias(table.getAlias());
        col.setCol(c);
        Expression e = new Expression();
        e.setColumn(col);
        out.setExpression(e);
        return out;
    }

    private boolean hasOut(Table t, String column) {

        return t.getOut().stream().filter(c -> c.getExpression().getColumn() != null)
                .map(o -> o.getExpression().getColumn())
                .anyMatch(c -> c.getCol().equals(column));
    }
}
