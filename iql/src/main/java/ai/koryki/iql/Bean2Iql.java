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

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

public class Bean2Iql {

    private Query query;

    public Bean2Iql(Query query) {
        this.query = query;
    }

    @Override
    public String toString() {
        return toString(query, 0);
    }

    private String toString(Query query, int indent) {

        StringBuilder b = new StringBuilder();

        if (query.getDescription() != null) {
            b.append("//" + query.getDescription().replace(System.lineSeparator(), System.lineSeparator() + "//"));
            b.append(System.lineSeparator());
            b.append(System.lineSeparator());
        }

        if (!query.getBlock().isEmpty()) {
            b.append(indent(indent) + "WITH ");
            b.append(toBlock(query.getBlock(), indent));
        }
//        if (script.getCte() != null && !script.getCte().isEmpty()) {
//            b.append(indent(indent) + "WITH ");
//            b.append(toString(script.getCte(), indent));
//        }
        b.append(toString(query.getSet(), indent));
        return b.toString();
    }

    private String toString(Set set, int indent) {

        if (set.getSelect() != null) {
            return toString(set.getSelect(), indent);
        } else {
            StringBuilder b = new StringBuilder();

            b.append(indent(indent) + toString(set.getLeft(), indent));
            //b.append(System.lineSeparator());
            b.append(indent(indent) + set.getOperator());
            b.append(System.lineSeparator());
            b.append(indent(indent) + toString(set.getRight(), indent));

            return b.toString();
        }
    }

    private String toString(Select select, int indent) {
        StringBuilder b = new StringBuilder();
        b.append(indent(indent) + "SELECT" + System.lineSeparator());
        b.append(toString(select.getStart(), indent + 1, false));
        b.append(toJoin(select.getJoin(), indent));

        if (select.getFilter() != null || select.getHaving() != null) {
            b.append(indent(indent) + "ALL ");
            b.append(toFilter(select.getFilter(), "FILTER", indent));
            b.append(toHaving(select.getHaving(), "HAVING", indent));
        }

        if (select.getLimit() > 0) {

            b.append(indent(indent) + "LIMIT " + select.getLimit());
            b.append(System.lineSeparator());
        }
        return b.toString();
    }

    private String toJoin(List<Join> join, int indent) {

        StringBuilder b = new StringBuilder();
        b.append(join.stream().map(j -> toString(j, indent + 2)).collect(Collectors.joining()));
        return b.toString();
    }

    private String toString(Join join, int indent) {
        StringBuilder b = new StringBuilder();
        b.append(indent(indent) + "JOIN ");
        if (join.isOptional()) {
            b.append("OPTIONAL ");
        }
        if (join.isInvers()) {
            b.append("INVERS ");
        }
        b.append(quoted(join.getCrit()));
        b.append(toString(join.getTable(), indent + 1, true));

        b.append(toJoin(join.getJoin(), indent + 1));
        b.append(indent(indent) + "OWNER");
        b.append(System.lineSeparator());

        return b.toString();
    }

    private String toString(Table table, int indent, boolean inline) {
        StringBuilder b = new StringBuilder();
        b.append(indent(inline ? 1 : indent) + quoted(table.getName()));
        if (table.getAlias() != null) {
            b.append(" " + quoted(table.getAlias()));
        }
        b.append(System.lineSeparator());
        b.append(toOut(table.getOut(), indent + 1));

        b.append(toFilter(table.getFilter(), indent + 1));
        b.append(toGroup(table.getGroup(), indent + 1));
        b.append(toHaving(table.getHaving(), indent + 1));

        b.append(toOrder(table.getOrder(), indent + 1));
        return b.toString();
    }

    private String toOut(List<Out> out, int indent) {

        if (out.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();

        b.append(indent(indent));
        b.append(out.stream().map(o -> toOut(o, indent + 1)).collect(Collectors.joining( System.lineSeparator() + indent(indent))));
        b.append(System.lineSeparator());
        return b.toString();
    }

    private String toGroup(List<Group> group, int indent) {
        if (group.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();

        b.append(indent(indent));
        b.append(group.stream().map(o -> toGroup(o, indent + 1)).collect(Collectors.joining(System.lineSeparator() + indent(indent))));
        b.append(System.lineSeparator());

        return b.toString();
    }

    private String toOrder(List<Order> order, int indent) {
        if (order.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();

        b.append(indent(indent));
        b.append(order.stream().map(o -> toOrder(o, indent + 1)).collect(Collectors.joining(System.lineSeparator() + indent(indent))));
        b.append(System.lineSeparator());

        return b.toString();
    }

    private String toOut(Out out, int indent) {
        StringBuilder b = new StringBuilder();
        b.append("OUT ");
        b.append(toString(out.getExpression(), indent));
        if (out.getHeader() != null) {
            b.append(" " + quoted(out.getHeader()));
        }
        if (out.getIdx() > 0) {
            b.append(" " + out.getIdx());
        }
        return b.toString();
    }

    private String toHaving(LogicalExpression expression, int indent) {
        return toHaving(expression, "HAVING", indent);
    }

    private String toHaving(LogicalExpression expression, String keyword, int indent) {

        if (expression == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        b.append(indent(indent));
        b.append(keyword + " " );
        b.append(System.lineSeparator());
        b.append(toString(expression, indent, true));
        b.append(System.lineSeparator());

        return b.toString();
    }

    private String toFilter(LogicalExpression expression, int indent) {
        return toFilter(expression, "FILTER", indent);
    }

    private String toFilter(LogicalExpression expression, String keyword, int indent) {

        if (expression == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        b.append(indent(indent));
        b.append(keyword + " ");
        b.append(System.lineSeparator());
        b.append(toString(expression, indent, true));
        b.append(System.lineSeparator());
        return b.toString();
    }

    private String toString(LogicalExpression expression, int indent, boolean leading) {

        if (expression == null) {
            return "";
        }

        String prefix = "";
        if (leading) {
            prefix = indent(indent + 1);
        }

        if (expression.isNot()) {
            return prefix + "NOT (" + toString(expression.getChildren().get(0), indent, false) + ")";
        } else if (expression.isValue()) {
            return prefix + toString( expression.getUnaryRelationalExpression(), indent);
        } else {
            StringBuilder b = new StringBuilder();

            String delim = System.lineSeparator() + indent(indent) +  expression.getType().name() + System.lineSeparator() + indent(indent + 1);

            b.append(expression.getChildren().stream().map(e -> toString(e, indent, false)).collect(Collectors.joining(delim)));

            return prefix + b.toString();
        }
    }

    private String toString(UnaryLogicalExpression expression, int indent) {

        if (expression == null) {
            return "";
        }

        StringBuilder b = new StringBuilder();

        if (expression.getExists() != null) {
            b.append(toString(expression.getParent(), expression.getExists(), indent));
        } else if (expression.getNode() != null) {
            return "(" + toString(expression.getNode(), indent, false) + ")";
        } else {

            b.append(toString(expression.getLeft(), indent));
            b.append(" " + expression.getOp());
            if (!expression.getRight().isEmpty()) {
                b.append(" ");
            }
            if (Bean2Sql.isSet(expression.getOp())) {
                b.append("(");
                b.append(toString(expression.getRight(), indent));
                b.append(")");
            } else if (Bean2Sql.isInterval(expression.getOp())) {
                b.append(toString(expression.getRight().get(0), expression.getRight().get(1), indent));
            } else {
                b.append(toString(expression.getRight(), indent));
            }
        }
        return b.toString();
    }

    private String toString(Expression lower, Expression upper, int indent) {
        StringBuilder b = new StringBuilder();
        b.append(toString(lower, indent));
        b.append(" AND ");
        b.append(toString(upper, indent));
        return b.toString();
    }

    private String toString(Expression expression, int indent) {
        StringBuilder b = new StringBuilder();

        if (expression.getColumn() != null) {
            b.append(toString(expression.getColumn(), indent));
        } else if (expression.getText() != null) {
            return expression.getText();
        } else if (expression.getNumber() != null) {
            DecimalFormat df = new DecimalFormat("#.######");  // Up to 2 decimals
            return df.format(expression.getNumber());
        } else if (expression.getLocalDateTime() != null) {
            return "TIMESTAMP '" + expression.getLocalDateTime() + "'";
        } else if (expression.getLocalDate() != null) {
            return "DATE '" + expression.getLocalDate() + "'";
        } else if (expression.getLocalTime() != null) {
            return "TIME '" + expression.getLocalTime() + "'";
        } else if (expression.getFunction() != null) {
            return toString(expression.getFunction(), indent);
        } else if (expression.getSet() != null) {
            b.append(toString(expression.getSet(), indent + 1));
        } else if (expression.getOperator() != null) {
            b.append(toString(expression.getLeft(), indent));
            b.append(" " + expression.getOperator() + " ");
            b.append(toString(expression.getRight(), indent));
        } else if (expression.getOperator() == null && expression.getLeft() != null) {
            b.append("(");
            b.append(toString(expression.getLeft(), indent));
            b.append(")");
        } else {
            throw new RuntimeException();
        }

         return b.toString();
    }
    private String toString(Function function, int indent) {
        StringBuilder b = new StringBuilder();
        b.append(function.getFunc());
        b.append("(");
        b.append(function.getArguments().stream().map(
                a -> toString(a, indent)).collect(Collectors.joining(", ")));
        b.append(")");

        return b.toString();
    }

    private String toString(Column column, int indent) {

        StringBuilder b = new StringBuilder();
        if (column.getAlias() != null) {
            b.append(quoted(column.getAlias()) + ".");
        }
        b.append(quoted(column.getCol()));

        return b.toString();
    }

    private String toString(List<Expression> expression, int indent) {
        StringBuilder b = new StringBuilder();
        b.append(expression.stream().map(e -> toString(e, indent)).collect(Collectors.joining(", ")));
        return b.toString();
    }

    private String toString(String parent, Exists exists, int indent) {
        StringBuilder b = new StringBuilder();

        b.append("EXISTS (" + System.lineSeparator());

        b.append(indent(indent + 1) + quoted(exists.getCrit()) + toString(exists.getTable(), indent, true));
        b.append(toJoin(exists.getJoin(), indent + 1));

        b.append(indent(indent));
        b.append(") " + (exists.getAlias() != null ? quoted(exists.getAlias()) : "") + System.lineSeparator());
        return b.toString();
    }

    private String toOrder(Order order, int indent) {
        StringBuilder b = new StringBuilder();
        b.append("ORDER ");
        if (order.getExpression() != null) {
            b.append(toString(order.getExpression(), indent));
        } else if (order.getHeader() != null) {
            b.append(" " + order.getHeader());
        }
        b.append(indent(indent) + " ");
        b.append(order.isAsc() ? "ASC" : "DESC");
        return b.toString();
    }

    private String toGroup(Group group, int indent) {
        StringBuilder b = new StringBuilder();
        b.append("GROUP ");
        b.append(toString(group.getExpression(), indent));

        return b.toString();
    }

    private String toBlock(List<Block> list, int indent) {
        StringBuilder b = new StringBuilder();
            b.append( list.stream().map(block -> quoted(block.getId()) + " AS ("
                    + System.lineSeparator() +
                    toString(block.getSet(), indent + 1) + indent(indent) + ")").collect(Collectors.joining("," + System.lineSeparator())));
            if (b.length() > 0) {
                b.append(System.lineSeparator());
            }

        return b.toString();
    }

//    private String toString(LinkedHashMap<String, Set> map, int indent) {
//        StringBuilder b = new StringBuilder();
//
//        b.append( map.entrySet().stream().map(e -> quoted(e.getKey()) + " AS ("
//                + System.lineSeparator() +
//                toString(e.getValue(), indent + 1) + indent(indent) + ")").collect(Collectors.joining("," + System.lineSeparator())));
//        if (b.length() > 0) {
//            b.append(System.lineSeparator());
//        }
//        return b.toString();
//    }

    private String indent(int l) {
        return Identifier.indent(l);
    }

    private String quoted(String text) {

        if (text == null) {
            return "";
        }

        return Identifier.normal(Identifier.quoted, text);
    }
}
