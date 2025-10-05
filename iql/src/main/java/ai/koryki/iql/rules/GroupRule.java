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

import ai.koryki.iql.Bean2Sql;
import ai.koryki.iql.query.*;

import java.util.List;

/**
 * Add GROUP-Expression, if aggregats are present.
 */
public class GroupRule {

    private Query query;

    public GroupRule(Query query) {
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

       List<Out> list = Bean2Sql.collectOut(select);
       if (hasAggregate(list) || hasHaving(select)) {

           list.forEach(o -> {
               if (!isAggregat(o)) {

                   Group g = new Group();
                   g.setExpression(o.getExpression());
                   select.getStart().getGroup().add(g);
               }
           });
       }
    }

    public static boolean hasHaving(Select select) {

        if (select.getHaving() != null) {
            return true;
        }
        if (hasHaving(select.getStart())) {
            return true;
        }

        return select.getJoin().stream().anyMatch(j -> hasHaving(j));
    }

    public static boolean hasHaving(Table table) {
        return table.getHaving() != null;
    }

    public static boolean hasHaving(Join join) {

        if (hasHaving(join.getTable())) {
            return true;
        }
        return join.getJoin().stream().anyMatch(j -> hasHaving(j));
    }

    public static boolean hasAggregate(List<Out> list) {

        return list.stream().map(o -> isAggregat(o)).anyMatch(b -> b);
    }

    public static boolean isAggregat(Out out) {
        return isAggregat(out.getExpression());
    }

    public static boolean isAggregat(Expression expression) {

        if (expression.getFunction() != null) {

            return Function.fromString(
                    expression.getFunction().getFunc())
                    .map(f -> f.isAggregat()).orElse(false);
        }
        return false;
    }
}
