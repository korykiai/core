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

import ai.koryki.antlr.Bag;
import ai.koryki.iql.DefaultVisitor;
import ai.koryki.iql.Visitor;
import ai.koryki.iql.Walker;
import ai.koryki.iql.query.*;
import ai.koryki.iql.query.Function;
import ai.koryki.model.schema.Schema;

import java.util.Deque;
import java.util.Optional;

/**
 * Replace Identity-Indicator with first PK-Column.
 * This is used for count(ID).
 */
public class IdentityRule {

    private final Schema db;

    public IdentityRule(Schema db) {
        this.db = db;
    }

    public void apply(Query query) {

        IdentityVisitor v = new IdentityVisitor(db);
        new Walker().walk(query, v);
    }

    private static class IdentityVisitor extends DefaultVisitor {

        private final Schema db;

        public IdentityVisitor(Schema db) {
            this.db = db;
        }

        @Override
        public void visit(Deque<Object> deque, Expression expression) {

            if (expression.getIdentity() != null) {
                Visitor.getNthElement(deque, 1).map(e -> e instanceof Function ? (Function)e : null).ifPresent(f -> {

                Table table = Visitor.table(deque, expression.getIdentity());

                    Bag<ai.koryki.model.schema.Column> col = new Bag<>();
                    Optional<ai.koryki.model.schema.Table> o = db.getTable(table.getName());
                    o.ifPresent(t -> col.setItem(t.getColumns().stream().filter(
                            c -> c.getPkPos() == 1).findFirst().orElse(null)));

                    Column c = new Column();
                    c.setAlias(table.getAlias());
                    c.setCol(col.getItem().getName());
                    expression.setColumn(c);
                    expression.setIdentity(null);
                });
            }
        }
    }
}
