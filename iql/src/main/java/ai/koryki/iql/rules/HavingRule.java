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

import ai.koryki.iql.query.*;
import ai.koryki.iql.logic.NodeType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Move LogicalExpressions from filter to having, if aggregats are present.
 */
public class HavingRule {

    private Query query;

    public HavingRule(Query query) {
        this.query = query;
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

        LogicalExpression filter = select.getFilter();

        if (filter == null) {
            return;
        }

        NodeType t = filter.getType();

        if (t.equals(NodeType.VAR)) {

            if (isHaving(filter)) {
                select.setFilter(null);
                select.setHaving(filter);
            }
        } else if (t.equals(NodeType.AND)) {
            List<LogicalExpression> c = filter.getChildren();

            List<LogicalExpression> havings = c.stream().filter(l -> isHaving(l)).collect(Collectors.toList());

            c.removeIf(l -> isHaving(l));

            if (!havings.isEmpty()) {
                LogicalExpression having = LogicalExpression.and(havings);
                select.setHaving(LogicalExpression.and(having, select.getHaving()));
            }
        }
    }

    private boolean isHaving(LogicalExpression logical) {
        boolean h =
        logical.isValue() &&
        logical.getUnaryRelationalExpression().getOp() != null &&
                (GroupRule.isAggregat(logical.getUnaryRelationalExpression().getLeft()));
        return h;
    }
}
