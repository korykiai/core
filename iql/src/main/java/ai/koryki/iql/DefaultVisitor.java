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

import ai.koryki.iql.query.*;

import java.util.Deque;

public class DefaultVisitor implements Visitor {
    @Override
    public void visit(Deque<Object> deque, Query query) {

    }

    @Override
    public void visit(Deque<Object> deque, Set set) {

    }

    @Override
    public void visit(Deque<Object> deque, Select select) {

    }

    @Override
    public void visit(Deque<Object> deque, Join join) {

    }

    @Override
    public void visit(Deque<Object> deque, Table table) {

    }

    @Override
    public void visit(Deque<Object> deque, Out out) {

    }

    @Override
    public void visit(Deque<Object> deque, LogicalExpression logicalExpression) {

    }

    @Override
    public void visit(Deque<Object> deque, UnaryLogicalExpression logicalExpression) {

    }

    @Override
    public void visit(Deque<Object> deque, Group group) {

    }

    @Override
    public void visit(Deque<Object> deque, Order order) {

    }

    @Override
    public void visit(Deque<Object> deque, Expression expression) {

    }

    @Override
    public void visit(Deque<Object> deque, Function function) {

    }

    @Override
    public void visit(Deque<Object> deque, Column column) {

    }

    @Override
    public void visit(Deque<Object> deque, Exists column) {

    }
}
