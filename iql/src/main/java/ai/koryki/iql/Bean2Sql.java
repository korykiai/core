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

import ai.koryki.iql.query.Function;
import ai.koryki.iql.rules.*;
import ai.koryki.iql.query.*;
import ai.koryki.iql.query.Set;
import ai.koryki.model.schema.Relation;
import ai.koryki.iql.logic.Normalizer;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Bean2Sql {
    private RelationResolver resolver;
    private Map<String, Table> idToTableMap;

    private Identifier idendifier = Identifier.lowercase;
    private Query query;

    public Bean2Sql(RelationResolver resolver, Query query) {
        this.resolver = resolver;
        this.query = query;

        // resolve block root tables
        idToTableMap = idToTableMap(query.getBlock());
    }


    public static Map<String, Table> idToTableMap(List<Block> list) {

        Map<String, Table> idToTableMap = new HashMap<>();

        list.forEach(b -> idToTableMap.put(b.getId(), getLeading(b.getSet())));

        return idToTableMap;
    }

    private static Table getLeading(Set set) {
        if (set.getLeft() != null) {
            return getLeading(set.getLeft());
        } else {
            return set.getSelect().getStart();
        }
    }

    public String toSql() {

        return toSql(query, 0);
    }

    public String toEnhancedSql() {
        applyRules();
        return toSql(query, 0);
    }

    public void applyRules() {
        new BlockRelationRule(query, resolver, idToTableMap).apply();
        new HavingRule(query).apply();
        new GroupRule(query).apply();
        new IdentityRule(resolver.getDb()).apply(query);

        new PushLogicalExpressionRule(resolver.getDb()).apply(query);
        new CheckOuterJoinFilterRule().apply(query);
    }

    protected String toSql(Query s, int indent) {
        StringBuilder b = new StringBuilder();

        if (s.getDescription() != null) {
            b.append("--" + s.getDescription().replace(System.lineSeparator(), System.lineSeparator() + "--"));
            b.append(System.lineSeparator());
            b.append(System.lineSeparator());
        }

        b.append(toSql(s.getBlock(), indent));

       // b.append(toSql(s.getCte(), indent));
        b.append(toSql(s.getSet(), indent));
        return b.toString();
    }

    protected String toSql(List<Block> cte, int indent) {

        if (cte.isEmpty()) {
            return "";
        }

        StringBuilder b = new StringBuilder();

        b.append(Identifier.indent(indent) + "WITH ");

        b.append(cte.stream().map(
                e -> toSql(e.getId(), e.getSet(), indent)).collect(Collectors.joining(System.lineSeparator() + ", ")));
        b.append(System.lineSeparator());

        return b.toString();
    }

    protected String toSql(String alias, Set set, int indent) {
        StringBuilder b = new StringBuilder();
        b.append(normal(alias));

        //b.append(" (" + cteColumnList(cteSelect(set)) + ")");

        b.append(" AS (");
        b.append(System.lineSeparator());

        if (set.getOperator() != null) {
            b.append(toSql(set.getLeft(), indent + 1));
            b.append(indent(indent) + set.getOperator() + System.lineSeparator());
            b.append(toSql(set.getRight(), indent + 1));
        } else {
            b.append(toSql(set.getSelect(), indent));
        }
        b.append(")");
        return b.toString();
    }

    protected String cteColumnList(Select select) {

        List<Out> out = collectOut(select);

        return out.stream().map(o -> normal(o.getHeader())).collect(Collectors.joining(", "));
    }

    protected Select cteSelect(Set set) {

        Select select;
        if (set.getOperator() != null) {
            return cteSelect(set.getLeft());
        } else {
            return set.getSelect();
        }
    }

    protected String toSql(Select select, int indent) {
        StringBuilder b = new StringBuilder();

        b.append(selectClause(select, indent));
        b.append(fromClause(select, indent));
        b.append(whereClause(select, indent));

        b.append(groupbyClause(select, indent));
        b.append(havingClause(select, indent));
        b.append(orderbyClause(select, indent));

        if (select.getLimit() > 0) {
            b.append(indent(indent));
            b.append("FETCH FIRST " + select.getLimit() + " ROWS ONLY");
            b.append(System.lineSeparator());
        }
        return b.toString();
    }

    private String groupbyClause(Select select, int indent) {
        return groupbyClause(select.getStart(), select.getJoin(), indent);
    }

    private String groupbyClause(Table start, List<Join> join, int indent) {

        List<Group> list = new ArrayList<>();
        if (start.getGroup() != null) {
            list.addAll(start.getGroup());
        }
        list.addAll(collectGroup(join));
        String group = list.stream().map(o -> toSql(o, indent + 1)).collect(Collectors.joining( System.lineSeparator()+ ", "));

        if (group.length() > 0) {
            StringBuilder b = new StringBuilder();
            b.append(indent(indent) + "GROUP BY");
            b.append(System.lineSeparator());
            b.append(indent(indent+ 2) + group);
            b.append(System.lineSeparator());
            return b.toString();
        }
        return "";
    }

    protected String toSql(Group group, int indent) {
        StringBuilder b = new StringBuilder();

        b.append(toSql(group.getExpression(), indent));
        return b.toString();
    }

    private String orderbyClause(Select select, int indent) {
        return orderbyClause(select.getStart(), select.getJoin(), indent);
    }

    private String orderbyClause(Table start, List<Join> join, int indent) {

        List<Order> list = new ArrayList<>();
        if (start.getOrder() != null) {
            list.addAll(start.getOrder());
        }
        list.addAll(collectOrder(join));

        String order = list.stream().map(o -> toSql(o, indent + 1)).collect(Collectors.joining( System.lineSeparator()+ ", "));

        if (order.length() > 0) {
            StringBuilder b = new StringBuilder();
            b.append(indent(indent) + "ORDER BY");
            b.append(System.lineSeparator());
            b.append(indent(indent + 2) + order);
            b.append(System.lineSeparator());
            return b.toString();
        }
        return "";
    }

    protected String toSql(Order order, int indent) {
        StringBuilder b = new StringBuilder();

        if (order.getExpression() != null) {
            b.append(toSql(order.getExpression(), indent));
        } else {
            b.append(order.getHeader());
        }
        b.append(" " + (order.isAsc() ? "ASC" : "DESC"));
        return b.toString();
    }

    private String havingClause(Select select, int indent) {
        String w = havingClause(select.getStart(), select.getJoin(), select.getHaving(), indent);

        if (!w.isEmpty()) {
            String r = indent(indent) + "HAVING" + System.lineSeparator();
            return r + w;
        }
        return "";
    }


    private String havingClause(Table start, List<Join> join, int indent) {

        List<LogicalExpression> nodes = new ArrayList<>();
        if (start.getHaving() != null) {
            nodes.add(start.getHaving());
        }
        nodes.addAll(collectHaving(join));

        String delimiter = System.lineSeparator() + indent(indent + 1) + "AND" + System.lineSeparator();
        String h = nodes.stream().map(e ->
                toSql(start, e, indent, true))
                .collect(Collectors.joining(
                        delimiter));

        if (h.length() > 0) {
            StringBuilder b = new StringBuilder();
            b.append(indent(indent) + "HAVING");
            b.append(System.lineSeparator() + indent(indent) + h);
            b.append(System.lineSeparator());
            return b.toString();
        }
        return "";
    }

    private String whereClause(Select select, int indent) {
        String w = whereClause(select.getStart(), select.getJoin(), select.getFilter(), indent);

        if (!w.isEmpty()) {
            String r = indent(indent) + "WHERE" + System.lineSeparator();
            return r + w;
        }
        return "";
    }

    private String whereClause(Table start, List<Join> join, LogicalExpression filter, int indent) {
        StringBuilder b = new StringBuilder();

        List<LogicalExpression> filters = new ArrayList<>();
        if (start.getFilter() != null) {
            filters.add(start.getFilter());
        }
        filters.addAll(collectInnerFilter(join));
        if (filter != null) {
            filters.add(filter);
        }

        // create one unique and-expression and normalize it.
        LogicalExpression all = LogicalExpression.and(filters);
        all = Normalizer.normalize(all);
        b.append(toSql(start, all, indent, true));

        if (b.length() > 0) {
            b.append(System.lineSeparator());
        }

        return b.toString();
    }

    private String havingClause(Table start, List<Join> join, LogicalExpression having, int indent) {
        StringBuilder b = new StringBuilder();

        List<LogicalExpression> havings = new ArrayList<>();
        if (start.getHaving() != null) {
            havings.add(start.getHaving());
        }
        havings.addAll(collectHaving(join));
        if (having != null) {
            havings.add(having);
        }

        // create one unique and-expression and normalize it.
        LogicalExpression all = LogicalExpression.and(havings);
        all = Normalizer.normalize(all);
        b.append(toSql(start, all, indent, true));

        if (b.length() > 0) {
            b.append(System.lineSeparator());
        }

        return b.toString();
    }

    protected List<LogicalExpression> collectInnerFilter(List<Join> join) {
        List<LogicalExpression> l = new ArrayList<>();
        for (Join j : join) {
            if (!j.isOptional()) {
                if (j.getTable().getFilter() != null) {

                    l.add(j.getTable().getFilter());
                }
                l.addAll(collectInnerFilter(j.getJoin()));
            }
        }
        return l;
    }

    protected List<LogicalExpression> collectHaving(List<Join> join) {
        List<LogicalExpression> l = new ArrayList<>();
        for (Join j : join) {
            if (j.getTable().getHaving() != null) {

                l.add(j.getTable().getHaving());
            }
            l.addAll(collectHaving(j.getJoin()));
        }
        return l;
    }

    protected List<Group> collectGroup(List<Join> join) {
        List<Group> l = new ArrayList<>();
        for (Join j : join) {
            l.addAll(j.getTable().getGroup());
            l.addAll(collectGroup(j.getJoin()));
        }
        return l;
    }

    protected List<Order> collectOrder(List<Join> join) {
        List<Order> l = new ArrayList<>();
        for (Join j : join) {
            l.addAll(j.getTable().getOrder());
            l.addAll(collectOrder(j.getJoin()));
        }
        return l;
    }

    private String fromClause(Select select, int indent) {
        return fromClause(select.getStart(), select.getJoin(), indent);
    }

    private String fromClause(Table start, List<Join> join, int indent) {
        StringBuilder b = new StringBuilder();
        b.append(indent(indent) + "FROM");
        b.append(System.lineSeparator());
        b.append(indent(indent + 1) +  toSql(start, indent + 1));
        b.append(System.lineSeparator());
        b.append(toSql(start, join, indent + 1));
        return b.toString();
    }

    private String selectClause(Select select, int indent) {
        StringBuilder b = new StringBuilder();
        b.append(indent(indent) + "SELECT");
        b.append(System.lineSeparator());

        List<Out> out = collectOut(select);

        if (out.isEmpty()) {
            b.append(indent(indent + 2) + "1" + System.lineSeparator());
        } else {
            b.append(indent(indent + 2) + selectClause(out, indent));
        }
        return b.toString();
    }

    public static List<Out> collectOut(Set set) {

        if (set.getSelect() != null) {
            return collectOut(set.getSelect());
        } else {
            return collectOut(set.getLeft());
        }
    }



    public static List<Out> collectOut(Select select) {
        List<Out> out = new ArrayList<>();
        out.addAll(select.getStart().getOut());
        out.addAll(collectOut(select.getJoin()));
        return out;
    }

    private String selectClause(List<Out> out, int indent) {
        StringBuilder b = new StringBuilder();
        String s = out.stream().map(o -> toSql(o, indent)).collect(Collectors.joining( System.lineSeparator() + indent(indent) + ", "));
        b.append(s);
        b.append(System.lineSeparator());
        return b.toString();
    }

    public static List<Out> collectOut(List<Join> join) {
        List<Out> l = new ArrayList<>();
        for (Join j : join) {
            l.addAll(j.getTable().getOut());
            l.addAll(collectOut(j.getJoin()));
        }
        return l;
    }

    protected String toSql(Table table, int indent) {
        StringBuilder b = new StringBuilder();

        b.append(normal(table.getName()));
        if (table.getAlias() != null) {
            b.append(" " + normal(table.getAlias()));
        }
        //b.append(System.lineSeparator());
        return b.toString();
    }


    protected String toSql(Out out, int indent) {
        StringBuilder b = new StringBuilder();

        b.append(toSql(out.getExpression(), indent));
        if (out.getHeader() != null) {
            b.append(" AS " + normal(out.getHeader()));
        }
        return b.toString();
    }

    protected String toSql(Table left, List<Join> join, int indent) {
        StringBuilder b = new StringBuilder();

        for (Join j : join) {
            b.append(toSql(left, j, indent + 1));
            b.append(toSql(j.getTable(), j.getJoin(), indent + 2));
        }

        return b.toString();
    }

    protected String toSql(Table left, Join join, int indent) {
        StringBuilder b = new StringBuilder();

        b.append(indent(indent));
        if (join.isOptional()) {
            b.append("LEFT OUTER ");
        } else {
            b.append("INNER ");
        }
        b.append("JOIN ");
        b.append(toSql(join.getTable(), indent));
        b.append(" ON");
        b.append(System.lineSeparator());

        b.append(joinColumns(left, join, indent + 1));
        b.append(System.lineSeparator());

        if (join.isOptional() && join.getTable().getFilter() != null) {
            // compare on outer join go here

            String e =  toSql(join.getTable(), join.getTable().getFilter(), indent, true);

            if (!e.isEmpty()) {
                b.append(indent(indent) + "AND" + System.lineSeparator() + e);
                b.append(System.lineSeparator());
            }
        }
        return b.toString();
    }

    private String toSql(Table parent, LogicalExpression expression, int indent, boolean leading) {

        String prefix = "";
        if (leading) {
            prefix = indent(indent + 1);
        }

        if (expression.isNot()) {
            return prefix + "NOT (" + toSql(parent, expression.getChildren().get(0), indent + 1, false) + ")";
        } else if (expression.isValue()) {
            return prefix + toSql(parent, expression.getUnaryRelationalExpression(), indent + 1);
        } else {
            StringBuilder b = new StringBuilder();

            String delim = System.lineSeparator() + indent(indent +1) +  expression.getType().name() + System.lineSeparator();
            b.append(expression.getChildren().stream().map(e -> toSql(parent, e, indent + 1, true)).collect(Collectors.joining(delim)));
            return b.toString();
        }
    }

    private String toSql(Table parent, UnaryLogicalExpression unaryLogicalExpression, int indent) {

        if (unaryLogicalExpression.getExists() != null) {
            return System.lineSeparator() + toSql(parent, unaryLogicalExpression.getExists(), indent);
        } else if (unaryLogicalExpression.getNode() != null) {
            return "(" + System.lineSeparator() + toSql(parent, unaryLogicalExpression.getNode(), indent, false) + System.lineSeparator() + indent(indent) + ")";
        } else {
            StringBuilder b = new StringBuilder();

            b.append(toSql(unaryLogicalExpression.getLeft(), indent));
            b.append(" " + toOp(unaryLogicalExpression.getOp()));

            if (isInterval(unaryLogicalExpression.getOp())) {
                b.append(" " + toInterval(unaryLogicalExpression.getRight().get(0), unaryLogicalExpression.getRight().get(1), indent));
            } else if (isSet(unaryLogicalExpression.getOp())) {
                b.append(" (" + unaryLogicalExpression.getRight().stream().map(e -> toSql(e, indent)).collect(Collectors.joining(", ")) + ")");
            } else {
                if (!unaryLogicalExpression.getRight().isEmpty()) {
                    b.append(" " + unaryLogicalExpression.getRight().stream().map(e -> toSql(e, indent)).collect(Collectors.joining(" ")));
                }
            }
            return b.toString();
        }
    }

    public static boolean isSet(String op) {
        return "IN".equalsIgnoreCase(op);
    }

    public static boolean isInterval(String op) {
        return "BETWEEN".equalsIgnoreCase(op);
    }

    protected String toInterval(Expression left, Expression right, int indent) {
        return toSql(left, indent) + " AND " + toSql(right, indent);
    }

    protected String toOp(String op) {
        if ("ISNULL".equalsIgnoreCase(op)) {
            return "IS NULL";
        }
        return op;
    }

    private String toSql(Table left, Exists exists, int indent) {

        StringBuilder b = new StringBuilder();

        b.append("EXISTS (");
        b.append(System.lineSeparator());

        existsSubselect(left, exists, indent, b);

        b.append(indent(indent) + ")");

        return b.toString();
    }

    private void existsSubselect(Table left, Exists exists, int indent, StringBuilder b) {
        b.append(indent(indent + 1) + "SELECT");
        b.append(System.lineSeparator());
        b.append(indent(indent + 2) + "1");
        b.append(System.lineSeparator());

        b.append(fromClause(exists.getTable(), exists.getJoin(), indent));

        String w = whereClause(exists.getTable(), exists.getJoin(), null, indent);

        String j = joinCols(left, exists, indent + 1);

        if (w != null && !w.isEmpty()) {
            b.append(indent(indent) + "WHERE");
            b.append(System.lineSeparator());
            b.append(j);
            b.append(System.lineSeparator());
            b.append(indent(indent) + "AND");
            b.append(System.lineSeparator());
            b.append(w);
        } else {
            b.append(indent(indent) + "WHERE");
            b.append(System.lineSeparator());
            b.append(j);
        }

        b.append(groupbyClause(exists.getTable(), exists.getJoin(), indent));
        b.append(havingClause(exists.getTable(), exists.getJoin(), indent));
        b.append(orderbyClause(exists.getTable(), exists.getJoin(), indent));
    }

    private String joinCols(Table left, Exists exists, int indent) {
        String msg = left.getName() + (left.getAlias() != null ? " " + left.getAlias() : "");
        String crit = exists.getCrit();
        Table right = exists.getTable();
        Table first = exists.isInvers() ? right : left;
        Table second = exists.isInvers() ? left : right;
        return joinColumns(indent, first, second, crit, msg, right);
    }

    private String joinColumns(Table left, Join join, int indent) {

        String msg = left.getName() + (left.getAlias() != null ? " " + left.getAlias() : "");
        String crit = join.getCrit();
        Table right = join.getTable();

        Table start = join.isInvers() ? right : left;
        Table end = join.isInvers() ? left : right;

        return joinColumns(indent, start, end, crit, msg, right);
    }

    private String joinColumns(int indent, Table start, Table end, String crit, String msg, Table right) {
        String startTable = start.getName();
        String endTable = end.getName();

        boolean b1 = resolver.isTableInDatabase(startTable);
        Table s = idToTableMap.get(startTable);
        if (!b1 && s == null) {
            throw new RuntimeException("can't find start: " + startTable);
        }
        startTable = b1 ? startTable : s.getName();
        boolean b2 = resolver.isTableInDatabase(endTable);

        Table e = idToTableMap.get(endTable);
        if (!b2 && e == null) {
            throw new RuntimeException("can't find end: " + endTable);
        }
        endTable = b2 ? endTable : e.getName();

        Optional<Relation> o = resolver.find(Identifier.normal(Identifier.lowercase, startTable), Identifier.normal(Identifier.lowercase, endTable), crit);

        if (!o.isPresent()) {
            throw new RuntimeException(msg +  " " + crit + " " + right.getName());
        }
        Relation r = o.get();

        String firstQualifier = start.getAlias() != null ? strip(start.getAlias()) : strip(start.getName());
        String secondQualifier = end.getAlias() != null ?  strip(end.getAlias()) : strip(end.getName());

        StringBuilder b = new StringBuilder();
        b.append(indent(indent));

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < r.getStartColumns().size(); i++) {

            lines.add(
                    (firstQualifier != null ? firstQualifier + "." : "") +
                            r.getStartColumns().get(i) + " = " +
                            (secondQualifier != null ? secondQualifier + "." : "")
                            + r.getEndColumns().get(i));
        }
        b.append(
                lines.stream().collect(Collectors.joining(System.lineSeparator() + Identifier.indent(indent))));
        return b.toString();
    }

    protected String toSql(Expression expression, int indent) {
        if (expression.getSet() != null) {
            return toSql(expression.getSet(), indent);
        } else if (expression.getFunction() != null) {
            return toSql(expression.getFunction(), indent);
        } else if (expression.getOperator() != null) {
            StringBuilder b = new StringBuilder();
            b.append(toSql(expression.getLeft(), indent));
            b.append(expression.getOperator());
            b.append(toSql(expression.getRight(), indent));
            return b.toString();
        } else if (expression.getOperator() == null && expression.getLeft() != null) {
            StringBuilder b = new StringBuilder();
            b.append("(");
            b.append(toSql(expression.getLeft(), indent));
            b.append(")");
            return b.toString();
        } else if (expression.getText() != null) {
            String text = expression.getText();
            text = text.replace("\\'", "''");
            return text;
        } else if (expression.getNumber() != null) {
            DecimalFormat df = new DecimalFormat("#.######");  // Up to 2 decimals
            return df.format(expression.getNumber());
        } else if (expression.getLocalDate() != null) {
            return dateExpression(expression);
        } else if (expression.getLocalDateTime() != null) {
            return timestempExpression(expression);
        } else if (expression.getLocalTime() != null) {
            return timeExpression(expression);
        } else if (expression.getColumn() != null) {
            return toSql(expression.getColumn(), indent);
        } else {
            throw new RuntimeException();
        }
    }

    protected String timeExpression(Expression expression) {
        return "TIME '" + expression.getLocalTime() + "'";
    }

    protected String timestempExpression(Expression expression) {
        return "TIMESTAMP '" + expression.getLocalDateTime() + "'";
    }

    protected String dateExpression(Expression expression) {
        return "DATE '" + expression.getLocalDate() + "'";
    }

    protected String toSql(Column column, int indent) {
        StringBuilder b = new StringBuilder();
        if ( column.getAlias() != null) {
            b.append(normal(column.getAlias()) + ".");
        }
        b.append(normal(column.getCol()));
        return b.toString();
    }

    protected String toSql(Function function, int indent) {
        StringBuilder b = new StringBuilder();

        String operator =
        ai.koryki.iql.rules.Function.fromString(function.getFunc()).map(f -> f.getOperator()).orElse(null);

        if (operator != null) {

            //b.append("(");
            b.append(function.getArguments().stream().map(
                    a -> toSql(a, indent)).collect(Collectors.joining(" " + operator + " ")));
            //b.append(")");
        } else {


            b.append(function.getFunc());
            b.append("(");
            b.append(function.getArguments().stream().map(
                    a -> toSql(a, indent)).collect(Collectors.joining(", ")));
            b.append(")");
        }
        return b.toString();
    }

    protected String toSql(Set set, int indent) {

        if (set.getSelect() != null) {
            return toSql(set.getSelect(), indent);
        } else {
            StringBuilder b = new StringBuilder();

            b.append(toSql(set.getLeft(), indent + 1));
            b.append(set.getOperator());
            b.append(System.lineSeparator());
            b.append(toSql(set.getRight(), indent + 1));

            return b.toString();
        }
    }

    private String normal(String text) {
        return Identifier.normal(idendifier, text);
    }

    private String normal(int l, String text) {
        return indent(l) + normal(text);
    }

    private String indent(int l) {
        return Identifier.indent(l);
    }

    public static String strip(String text) {

        if (text == null) {
            return null;
        }

        String n = text.toLowerCase();

        if (n.startsWith("\"")) {
            n = n.substring(1);
        }
        if (n.endsWith("\"")) {
            n = n.substring(0, n.length() - 1);
        }
        return n;
    }

}