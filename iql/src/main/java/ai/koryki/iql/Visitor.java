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

import ai.koryki.antlr.Bag;
import ai.koryki.iql.query.*;

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public interface Visitor {

    void visit(Deque<Object> deque, Query query);
    void visit(Deque<Object> deque, Set set);
    void visit(Deque<Object> deque, Select select);
    void visit(Deque<Object> deque, Join join);
    void visit(Deque<Object> deque, Table table);
    void visit(Deque<Object> deque, Out out);
    void visit(Deque<Object> deque, LogicalExpression logicalExpression);
    void visit(Deque<Object> deque, UnaryLogicalExpression logicalExpression);
    void visit(Deque<Object> deque, Group group);
    void visit(Deque<Object> deque, Order order);
    void visit(Deque<Object> deque, Expression expression);
    void visit(Deque<Object> deque, Function function);
    void visit(Deque<Object> deque, Column column);
    void visit(Deque<Object> deque, Exists column);


    /**
     * Returns the n-th element from the deque.
     * n = 1 → element
     * n = 2 → second element
     *
     * @param deque the deque to read from
     * @param n     1-based index from the strat
     * @param <E>   element type
     * @return Optional with the n-th element or Optional.empty()
     */
    static <E> Optional<E> getNthElement(Deque<E> deque, int n) {
        if (deque == null || n <= 0 || n > deque.size()) {
            return Optional.empty();
        }

        Iterator<E> iter = deque.iterator();
        for (int i = 1; i < n; i++) {
            Object o = iter.next();
        }
        E result = iter.next();
        return Optional.of(result);
    }

    static <E> Select parentSelect(Deque<E> deque) {

        Iterator<E> iter = deque.iterator();
        while(true) {
            if (!iter.hasNext()) {
                return null;
            }
            Object e = iter.next();
            if (e instanceof Select) {
                return (Select)e;
            }
        }
    }

    static Table table(Select select, String alias) {

        Bag<Table> t = new Bag<>();
        Visitor v = new DefaultVisitor() {
            @Override
            public void visit(Deque<Object> deque, Table table) {
                if (table.getAlias().equals(alias)) {
                    t.setItem(table);
                }
            }
        };
        new Walker().walk(select, v);

        return t.getItem();
    }

    static <E> Table table(Deque<E> deque, String alias) {
        return table(parentSelect(deque), alias);
    }
}
