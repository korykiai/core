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
import ai.koryki.iql.query.Set;
import ai.koryki.iql.logic.Normalizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class KQL2Bean {

    private KQLParser.QueryContext script;
    private String description;

    public KQL2Bean(KQLParser.QueryContext script, String description) {
        this.script = script;
        this.description = description;
    }

    public Query toBean() {

        Query bean = new Query();
        bean.setDescription(description);
        if (script.block() != null) {
            bean.setBlock(toMap(script.block()));
        }
        bean.setSet(toSet(script.set()));
        return bean;
    }

    private List<Block> toMap(List<KQLParser.BlockContext> cte) {

        List<Block> map = new ArrayList<>();
        for (KQLParser.BlockContext b : cte) {
            Block block = new Block();
            block.setId(b.ID().getText());
            block.setSet(toSet(b.set()));
            map.add(block);
        }
        return map;
    }

    private Set toSet(KQLParser.SetContext set) {
        Set bean = new Set();

        String op = set.SET_INTERSECT() != null ? set.SET_INTERSECT().getText() :
                set.SET_MINUS() != null ? set.SET_MINUS().getText() :
                set.SET_UNION() != null ? set.SET_UNION().getText() :
                set.SET_UNIONALL() != null ? set.SET_UNIONALL().getText() : null;


        if (set.LEFT_PAREN() != null) {
            return toSet(set.set(0));
        } else if (op != null) {
            bean.setOperator(op);
            bean.setLeft(toSet(set.set(0)));
            bean.setRight(toSet(set.set(1)));
        } else if (set.select() != null) {
            bean.setSelect(toSelect(set.select()));
        } else {
            throw new RuntimeException();
        }

        return bean;
    }

    private Order toOrder(KQLParser.OrderContext order) {
        Order o = new Order();
        if (order.expression() != null) {
            o.setExpression(toExpression(order.expression()));
        } else if (order.header() != null) {
            o.setHeader(order.header().getText());
        }
        Boolean asc = asc(order.ASC());
        if (asc != null) {
            o.setAsc(asc);
        }

        return o;
    }

    private Select toSelect(KQLParser.SelectContext select) {

        Select bean = new Select();

        Table start = toTable(select.table());
        bean.setStart(start);
        HashMap<String, List<Join>> joins = new HashMap<>();

        // store first link
        joins.put(start.getAlias(), bean.getJoin());

        if (!select.link().isEmpty()) {
            for (KQLParser.LinkContext link : select.link()) {

                Join join = toJoin(link);
                List<Join> j = findStartLink(link, joins);
                j.add(join);
                joins.put(join.getTable().getAlias(), join.getJoin());
            }
        }

        if (select.filterClause() != null) {
            LogicalExpression node = toLogicalNode(select.filterClause().logical_expression());
            node = Normalizer.normalize(node);
            bean.setFilter(node);
        }
        if (select.fetchClause() != null) {
            int idx = 1;
            for (KQLParser.FetchItemContext r : select.fetchClause().fetchItem()) {
                Out o = toOut(r, idx);
                start.getOut().add(o);
                idx++;
            }
        }

        select.order().forEach(o -> bean.getStart().getOrder().add(toOrder(o)));

        if (select.limitClause() != null && select.limitClause().NUMBER() != null) {
            int limit = Integer.parseInt(select.limitClause().NUMBER().getText());
            if (limit > 0) {
                bean.setLimit(limit);
            }
        }
        return bean;
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

    public LogicalExpression toLogicalNode(KQLParser.Logical_expressionContext logicalExpression) {

        if (logicalExpression.unary_logical_expression() != null) {
            return LogicalExpression.value(toUnaryLogicalExpression(logicalExpression.unary_logical_expression()));
        } else if (logicalExpression.NOT() != null) {
            LogicalExpression n = toLogicalNode(logicalExpression.negate);
            return LogicalExpression.not(n);
        } else {
            LogicalExpression left = toLogicalNode(logicalExpression.left);
            LogicalExpression right = toLogicalNode(logicalExpression.right);
            return logicalExpression.AND() != null ? LogicalExpression.and(left, right) : LogicalExpression.or(left, right);
        }
    }

    public UnaryLogicalExpression toUnaryLogicalExpression(KQLParser.Unary_logical_expressionContext unaryLogicalExpressionContext) {

        if (unaryLogicalExpressionContext.logical_expression() != null) {
            UnaryLogicalExpression bean = new UnaryLogicalExpression();
            bean.setNode(toLogicalNode(unaryLogicalExpressionContext.logical_expression()));
            return bean;
        } else if (unaryLogicalExpressionContext.operator() != null) {
            UnaryLogicalExpression bean = new UnaryLogicalExpression();
            bean.setOp(unaryLogicalExpressionContext.operator().getText());
            bean.setLeft(toExpression(unaryLogicalExpressionContext.expression().get(0)));
            for (int i = 1; i < unaryLogicalExpressionContext.expression().size(); i++) {
                bean.getRight().add(toExpression(unaryLogicalExpressionContext.expression().get(i)));
            }
            return bean;
        } else {
            throw new RuntimeException();
        }
    }
    private Out toOut(KQLParser.FetchItemContext ret, int idx) {

        Out o = new Out();
        o.setIdx(idx);
        o.setExpression(toExpression(ret.expression()));
        if (ret.h != null) {
            o.setHeader(ret.h.getText());
        }
        return o;
    }

    private Expression toExpression(KQLParser.ExpressionContext expression) {

        if (expression.set() != null) {
            Expression bean = new Expression();
            bean.setSet(toSet(expression.set()));
            return bean;
        } else if (expression.LEFT_PAREN() != null) {
            Expression bean = new Expression();
            bean.setLeft(toExpression(expression.expression(0)));
            return bean;
        } else if (expression.MULT() != null) {
            String name = ai.koryki.iql.rules.Function.multiply.name();
            return mathFunction(expression, name);
        } else if (expression.DIV() != null) {
            String name = ai.koryki.iql.rules.Function.divide.name();
            return mathFunction(expression, name);
        } else if (expression.PLUS() != null) {
            String name = ai.koryki.iql.rules.Function.add.name();
            return mathFunction(expression, name);
        } else if (expression.MINUS() != null) {
            String name = ai.koryki.iql.rules.Function.minus.name();
            return mathFunction(expression, name);
        } else if (expression.date_literal() != null) {
            return toExpression(expression.date_literal());
        } else if (expression.column() != null) {
            Expression bean = new Expression();
            bean.setColumn(toColumn(expression.column()));
            return bean;
        } else if (expression.function() != null) {
            Expression bean = new Expression();
            bean.setFunction(toFunction(expression.function()));
            return bean;
        } else if (expression.NUMBER() != null) {
            Expression bean = new Expression();
            bean.setNumber(Double.valueOf(expression.NUMBER().getText()));
            return bean;
        } else if (expression.SQ_STRING() != null) {
            Expression bean = new Expression();
            bean.setText(expression.SQ_STRING().getText());
            return bean;
        } else {
            throw new RuntimeException();
        }
    }

    private Expression mathFunction(KQLParser.ExpressionContext expression, String name) {
        Expression bean = new Expression();
        Function f = new Function();
        f.setFunc(name);
        f.setArguments(Arrays.asList(toExpression(expression.left), toExpression(expression.right)));
        bean.setFunction(f);
        return bean;
    }

    private Expression toExpression(KQLParser.Date_literalContext date) {
        Expression bean = new Expression();

        if (date.TIME_FORMAT() != null) {
            bean.setLocalTime(LocalTime.parse(Identifier.unquote(date.TIME_FORMAT().getText())));
        } else if (date.TIMESTAMP_FORMAT() != null) {
            bean.setLocalDateTime(LocalDateTime.parse(Identifier.unquote(date.TIMESTAMP_FORMAT().getText())));
        } else if (date.DATE_FORMAT() != null) {
            bean.setLocalDate(LocalDate.parse(Identifier.unquote(date.DATE_FORMAT().getText())));
        }
        return bean;
    }

    private Column toColumn(KQLParser.ColumnContext column) {

        Column c = new Column();
        if (column.alias != null) {
            c.setAlias(column.alias.getText());
        }
        c.setCol(column.col.getText());

        return c;
    }

    private Function toFunction(KQLParser.FunctionContext function) {

        Function f = new Function();
        f.setFunc(function.ID().getText());

        for (KQLParser.ArgumentContext a : function.argument()) {
            if (a.expression() != null) {
                f.getArguments().add(toExpression(a.expression()));
            } else {
                Expression e = new Expression();
                e.setIdentity(a.identity.getText());
                f.getArguments().add(e);
            }
        }
        return f;
    }

    private List<Join> findStartLink(KQLParser.LinkContext link, HashMap<String, List<Join>> joins) {
        String from =  link.from.getText();
        List<Join> j = joins.get(from);
        return j;
    }

    private Join toJoin(KQLParser.LinkContext link) {

        String to = link.to.getText();
        String crit = link.crit != null ? link.crit.getText() : null;
        String alias = link.alias.getText();
        Join bean = new Join();
        bean.setCrit(crit);
        if (link.PLUS() != null) {
            bean.setOptional(true);
        }
        if (link.LESS() != null) {
            bean.setInvers(true);
        }

        Table t = new Table();
        t.setName(to);
        t.setAlias(alias);

        bean.setTable(t);
        return bean;
    }

    private Table toTable(KQLParser.TableContext table) {
        Table bean = new Table();

        bean.setAlias(table.alias.getText());
        bean.setName(table.name.getText());
        return bean;
    }
}
