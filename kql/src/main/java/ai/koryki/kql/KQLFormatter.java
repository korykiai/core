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
import ai.koryki.iql.Identifier;
import ai.koryki.iql.query.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.stream.Collectors;

public class KQLFormatter {

    private KQLParser.QueryContext script;
    private String description;

    public KQLFormatter(KQLParser.QueryContext script, String description) {
        this.script = script;
        this.description = description;
    }

    public String format() {
        StringBuilder b = new StringBuilder();
        if (description != null) {
                b.append("//" + description.replace(System.lineSeparator(), System.lineSeparator() + "//"));
                b.append(System.lineSeparator());
                b.append(System.lineSeparator());
        }
        if (!script.block().isEmpty()) {
            b.append(indent(0) + "WITH ");
            b.append(toMap(script.block(), 0));
        }
        b.append(toSet(script.set(), 0));
        return b.toString();
    }

    private String toMap(List<KQLParser.BlockContext> cte, int indent) {
        StringBuilder b = new StringBuilder();

        b.append( cte.stream().map(block -> block.ID() + " AS ("
                + System.lineSeparator() +
                toSet(block.set(), indent + 1) + indent(indent) + ")").collect(Collectors.joining("," + System.lineSeparator())));
        if (b.length() > 0) {
            b.append(System.lineSeparator());
        }
        return b.toString();
    }

    private String toSet(KQLParser.SetContext set, int indent) {
        StringBuilder b = new StringBuilder();
        String op = set.SET_INTERSECT() != null ? set.SET_INTERSECT().getText() :
                set.SET_MINUS() != null ? set.SET_MINUS().getText() :
                set.SET_UNION() != null ? set.SET_UNION().getText() :
                set.SET_UNIONALL() != null ? set.SET_UNIONALL().getText() : null;


        if (set.LEFT_PAREN() != null) {
            return "(" + toSet(set.set(0), indent) + ")";
        } else if (op != null) {
            b.append(toSet(set.set(0), indent));
            b.append(indent(indent) + op + System.lineSeparator());
            b.append(toSet(set.set(1), indent));
        } else if (set.select() != null) {
            b.append(toSelect(set.select(), indent));
        } else {
            throw new RuntimeException();
        }

        return b.toString();
    }

    private String toSelect(KQLParser.SelectContext select, int indent) {

        StringBuilder b = new StringBuilder();
        b.append(indent(indent) + "FIND ");

        b.append(toTable(select.table(), indent));

        StringBuilder   l = new StringBuilder();
        l.append(
        select.link().stream().map(j -> toJoin(j, indent)).collect(Collectors.joining(", ")));
        if (l.length() > 0) {
            b.append(", "+  l.toString());
        }
        b.append(System.lineSeparator());
        if (select.whereClause() != null) {
            String where = toLogicalNode(select.whereClause().logical_expression(), indent);
            if (where.length() > 0) {
                b.append(indent(indent) + "WHERE " + where);
                b.append(System.lineSeparator());
            }
        }
        String ret = select.returnClause().returnItem().stream().map(r -> toOut(r, indent)).collect(Collectors.joining(", "));
        if (ret.length() > 0) {
            b.append(indent(indent) + "RETURN " + ret);
            b.append(System.lineSeparator());
        }

        String order =
        select.order().stream().map(o -> toOrder(o, indent)).collect(Collectors.joining(", "));
        if (order.length() > 0) {
            b.append(indent(indent) + "ORDER " + order);
            b.append(System.lineSeparator());
        }

        //        if (select.orderClause() != null) {
//            KQLParser.OrderClauseContext order = select.orderClause();
//            int idx = 0;
//            for (ParseTree t : order.children) {
//
//                if (t instanceof KQLParser.ExpressionContext) {
//                    KQLParser.ExpressionContext e = (KQLParser.ExpressionContext)t;
//                    b.append(toExpression(e, indent));
//
//                    if (order.children.size() > idx + 1) {
//                        Boolean asc = asc(order.children.get(idx + 1));
//                        if (asc != null) {
//                            b.append(asc ? "ASC" : "DESC");
//                        }
//                    }
//                }
//                idx++;
//            }
//        }

        if (select.limitClause() != null && select.limitClause().NUMBER() != null) {
            int limit = Integer.parseInt(select.limitClause().NUMBER().getText());
            if (limit > 0) {
                b.append(indent(indent) + "LIMIT " + limit);
                b.append(System.lineSeparator());
            }
        }
        return b.toString();
    }

    private Boolean asc(ParseTree t) {
        if (t instanceof TerminalNode) {
            TerminalNode n = (TerminalNode)t;
            if (n.getSymbol().getType() == KQLParser.ASC || n.getSymbol().getType() == KQLParser.DESC) {
                return n.getSymbol().getType() == KQLParser.ASC;
            }
        }
        return null;
    }

    private String toOrder(KQLParser.OrderContext order, int indent) {

        String d = "";
        if (order.DESC() != null) {
            d = " "+  order.DESC().getText();
        } else if (order.ASC() != null) {
            d =" " + order.ASC().getText();
        }

        String o = order.expression() != null ? toExpression(order.expression(), indent) : " " + order.header().getText();

        return o + d;
    }

    private String toLogicalNode(KQLParser.Logical_expressionContext logicalExpression, int indent) {

        if (logicalExpression.unary_logical_expression() != null) {
            return toUnaryLogicalExpression(logicalExpression.unary_logical_expression(), indent);
        } else if (logicalExpression.NOT() != null) {
            return " NOT " + toLogicalNode(logicalExpression.negate, indent);

        } else {
            String left = toLogicalNode(logicalExpression.left, indent);
            String right = toLogicalNode(logicalExpression.right, indent);
            String op = logicalExpression.AND() != null ? "AND" : "OR";
            return left + " " + op + " " + right;
        }
    }

    private String toUnaryLogicalExpression(KQLParser.Unary_logical_expressionContext unaryLogicalExpressionContext, int indent) {

        if (unaryLogicalExpressionContext.logical_expression() != null) {
            return "(" + toLogicalNode(unaryLogicalExpressionContext.logical_expression(), indent)+ ")";
        } else if (unaryLogicalExpressionContext.operator() != null) {
            UnaryLogicalExpression bean = new UnaryLogicalExpression();
            String op = unaryLogicalExpressionContext.operator() != null ? unaryLogicalExpressionContext.operator().getText() : null;
            String left = toExpression(unaryLogicalExpressionContext.expression().get(0), indent);

            StringBuilder right = new StringBuilder();

            if (unaryLogicalExpressionContext.expression().size() > 1) {
                List<KQLParser.ExpressionContext> rl = unaryLogicalExpressionContext.expression().subList(1, unaryLogicalExpressionContext.expression().size());
                if (isInterval(op)) {
                    right.append(toInterval(rl.get(0), rl.get(1), indent));
                } else if (isSet(op)) {


                    boolean subselect = !rl.isEmpty() && rl.get(0).set() != null;
                    String intro = subselect ? System.lineSeparator() : "";
                    String extro = subselect ? indent(indent) : "";

                    right.append("(" + intro + rl.stream().map(e -> toExpression(e, indent + 1))
                            .collect(Collectors.joining(", ")) + extro + ")");
                } else if (!unaryLogicalExpressionContext.expression().isEmpty()) {
                    right.append(rl.stream().map(e -> toExpression(e, indent))
                            .collect(Collectors.joining(", ")));
                }
            }

            if (op == null) {
                return left;
            } else if (right.length() == 0) {
                return left + " " + op;
            } else {
                return left + " " + op + " " + right;
            }
        } else {
            throw new RuntimeException();
        }
    }

    public static boolean isSet(String op) {
        return "IN".equalsIgnoreCase(op);
    }

    public static boolean isInterval(String op) {
        return "BETWEEN".equalsIgnoreCase(op);
    }

    protected String toInterval(KQLParser.ExpressionContext left, KQLParser.ExpressionContext right, int indent) {
        return toExpression(left, indent) + " AND " + toExpression(right, indent);
    }


    private String toOut(KQLParser.ReturnItemContext ret, int indent) {
        StringBuilder b = new StringBuilder();

        b.append(toExpression(ret.expression(), indent));
        if (ret.h != null) {
            b.append(" " + ret.h.getText());
        }
        return b.toString();
    }

    private String toExpression(KQLParser.ExpressionContext expression, int indent) {

        if (expression.set() != null) {
            return toSet(expression.set(), indent);
        } else if (expression.LEFT_PAREN() != null) {
            return "(" + toExpression(expression.expression(0), indent) + " )";
        } else if (expression.MULT() != null) {
            String name = ai.koryki.iql.rules.Function.multiply.name();
            return mathFunction(expression, name, indent);
        } else if (expression.DIV() != null) {
            String name = ai.koryki.iql.rules.Function.divide.name();
            return mathFunction(expression, name, indent);
        } else if (expression.PLUS() != null) {
            String name = ai.koryki.iql.rules.Function.add.name();
            return mathFunction(expression, name, indent);
        } else if (expression.MINUS() != null) {
            String name = ai.koryki.iql.rules.Function.minus.name();
            return mathFunction(expression, name, indent);
        } else if (expression.date_literal() != null) {
            return toExpression(expression.date_literal());
        } else if (expression.column() != null) {
            Expression bean = new Expression();
            return toColumn(expression.column(), indent);
        } else if (expression.function() != null) {
            return toFunction(expression.function(), indent);
        } else if (expression.NUMBER() != null) {
            return expression.NUMBER().getText();
        } else if (expression.SQ_STRING() != null) {
            return expression.SQ_STRING().getText();
        } else {
            throw new RuntimeException();
        }
    }

    private String mathFunction(KQLParser.ExpressionContext expression, String name, int indent) {

        String left = toExpression(expression.left, indent);
        String right  = toExpression(expression.right, indent);
        String op = ai.koryki.iql.rules.Function.fromString(name).get().getOperator();
        return left + " " + op + " " + right;
    }

    private String toExpression(KQLParser.Date_literalContext date) {

        if (date.TIME_FORMAT() != null) {
            return "TIME "  + date.TIME_FORMAT().getText();
        } else if (date.TIMESTAMP_FORMAT() != null) {
            return "TIMESTAMP "  + date.TIMESTAMP_FORMAT().getText();
        } else if (date.DATE_FORMAT() != null) {
            return "DATE "  + date.DATE_FORMAT().getText();
        } else {
            throw new RuntimeException();
        }
    }

    private String toColumn(KQLParser.ColumnContext column, int indent) {

        StringBuilder b = new StringBuilder();

        Column c = new Column();
        if (column.alias != null) {
            b.append(column.alias.getText() + ".");
        }
        b.append(column.col.getText());

        return b.toString();
    }

    private String toFunction(KQLParser.FunctionContext function, int indent) {
        StringBuilder b = new StringBuilder();

        b.append(function.ID().getText());
        b.append("(");
        b.append(function.argument().stream().map(a -> toArgument(a, indent))
                .collect(Collectors.joining(", ")));
        b.append(")");
        return b.toString();
    }

    private String toArgument(KQLParser.ArgumentContext argument, int indent) {

        if (argument.expression() != null) {
            return toExpression(argument.expression(), indent);
        } else if (argument.identity != null) {
            return argument.identity.getText();
        } else {
            throw new RuntimeException();
        }
    }


    private String toJoin(KQLParser.LinkContext link, int indent) {

        StringBuilder b = new StringBuilder();

        b.append(link.from.getText());
        if (link.LESS() != null) {
            b.append("<");
        }
        String crit = link.crit != null ? link.crit.getText() : null;
        if (crit != null) {
            b.append("-");
            b.append(crit);
        }
        b.append(link.PLUS() != null ? "+" : "-");
        if (link.GREATER() != null) {
            b.append(">");
        }
        b.append(link.to.getText());
        b.append(" ");
        b.append(link.alias.getText());

        return b.toString();
    }

    private String toTable(KQLParser.TableContext table, int indent) {

        StringBuilder b = new StringBuilder();
        b.append(table.name.getText());
        b.append(" ");
        b.append(table.alias.getText());
        return b.toString();
    }

    private String indent(int l) {
        return Identifier.indent(l);
    }
}
