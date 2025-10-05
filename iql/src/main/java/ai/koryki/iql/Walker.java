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

import java.util.ArrayDeque;
import java.util.Deque;

public class Walker {

    private Deque<Object> deque = new ArrayDeque<>();

    public void walk(Query query, Visitor visitor) {

        visitor.visit(deque, query);
        deque.push(query);
        query.getBlock().forEach(b -> walk(b.getSet(), visitor));
        walk(query.getSet(), visitor);
        deque.pop();
    }

    public void walk(Set set, Visitor visitor) {

        visitor.visit(deque, set);
        deque.push(set);
        if (set.getOperator() != null) {
            walk(set.getLeft(), visitor);
            walk(set.getRight(), visitor);
        } else {
            walk(set.getSelect(), visitor);
        }
        deque.pop();
    }

    public void walk(Select select, Visitor visitor) {

        visitor.visit(deque, select);
        deque.push(select);
        walk(select.getStart(), visitor);

        if (select.getFilter() != null) {
            walk(select.getFilter(), visitor);
        }
        if (select.getHaving() != null) {
            walk(select.getHaving(), visitor);
        }

        select.getJoin().forEach(j -> walk(j, visitor));
        deque.pop();
    }

    public void walk(Join join, Visitor visitor) {

        visitor.visit(deque, join);
        deque.push(join);
        walk(join.getTable(), visitor);
        join.getJoin().forEach(j -> walk(j, visitor));
        deque.pop();
    }

    public void walk(Exists exists, Visitor visitor) {

        visitor.visit(deque, exists);
        deque.push(exists);
        walk(exists.getTable(), visitor);
        exists.getJoin().forEach(j -> walk(j, visitor));
        deque.pop();
    }

    public void walk(Table table, Visitor visitor) {

        visitor.visit(deque, table);
        deque.push(table);
        table.getOut().forEach(o -> walk(o, visitor));
        if (table.getFilter() != null) {
            walk(table.getFilter(), visitor);
        }
        if (table.getHaving() != null) {
            walk(table.getHaving(), visitor);
        }
        table.getGroup().forEach(g -> walk(g, visitor));
        table.getOrder().forEach(o -> walk(o, visitor));
        deque.pop();
    }

    public void walk(Out out, Visitor visitor) {

        visitor.visit(deque, out);
        deque.push(out);
        walk(out.getExpression(), visitor);
        deque.pop();
    }

    public void walk(Group group, Visitor visitor) {

        visitor.visit(deque, group);
        deque.push(group);
        walk(group.getExpression(), visitor);
        deque.pop();
    }

    public void walk(Order order, Visitor visitor) {

        visitor.visit(deque, order);
        deque.push(order);
        if (order.getExpression() != null) {
            walk(order.getExpression(), visitor);
        }
        deque.pop();
    }

    public void walk(LogicalExpression expression, Visitor visitor) {

        visitor.visit(deque, expression);
        deque.push(expression);
        if (expression.getUnaryRelationalExpression() != null) {
            walk(expression.getUnaryRelationalExpression(), visitor);
        }
        expression.getChildren().forEach(c -> walk(c, visitor));
        deque.pop();
    }

    public void walk(UnaryLogicalExpression expression, Visitor visitor) {

        visitor.visit(deque, expression);
        deque.push(expression);
        if (expression.getExists() != null)  {
            walk(expression.getExists(), visitor);
        }
        if (expression.getLeft() != null) {
            walk(expression.getLeft(), visitor);
        }
        if (expression.getNode() != null) {
            walk(expression.getNode(), visitor);
        }
        expression.getRight().forEach(e -> walk(e, visitor));
        deque.pop();
    }

    public void walk(Expression expression, Visitor visitor) {

        visitor.visit(deque, expression);
        deque.push(expression);
        if (expression.getLeft() != null) {
            walk(expression.getLeft(), visitor);
        }
        if (expression.getRight() != null) {
            walk(expression.getRight(), visitor);
        }
        if (expression.getSet() != null) {
            walk(expression.getSet(), visitor);
        }
        if (expression.getFunction() != null) {
            walk(expression.getFunction(), visitor);
        }
        if (expression.getColumn() != null) {
            walk(expression.getColumn(), visitor);
        }
        deque.pop();
    }

    public void walk(Function function, Visitor visitor) {

        visitor.visit(deque, function);
        deque.push(function);
        function.getArguments().forEach(a -> walk(a, visitor));
        deque.pop();
    }

    public void walk(Column column, Visitor visitor) {

        visitor.visit(deque, column);
        deque.push(column);
        deque.pop();
    }
}
