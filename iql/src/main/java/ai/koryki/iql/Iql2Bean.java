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

import ai.koryki.antlr.IQLParser;
import ai.koryki.antlr.iql.IQLReader;
import ai.koryki.iql.query.*;
import ai.koryki.iql.logic.Normalizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Iql2Bean {

    private IQLParser.QueryContext script;
    private String description;

    public Iql2Bean(IQLReader reader) {
        this(reader.getQuery(), reader.getDescription());
    }

    public Iql2Bean(IQLParser.QueryContext script, String description) {
        this.script = script;
        this.description = description;
    }

    public Query toScript() {

        Query bean = new Query();
        bean.setDescription(description);
        if (script.cte() != null) {
            bean.setBlock(toBlock(script.cte()));
        }
        bean.setSet(toSet(script.set()));
        return bean;
    }

    public List<Block> toBlock(IQLParser.CteContext cte) {

        List<Block> map = new ArrayList<>();
        for (int i = 0; i < cte.STRING().size(); i++) {
            String id = cte.STRING(i).getText();
            Set set = toSet(cte.set(i));

            Block block = new Block();
            block.setId(id);
            block.setSet(set);
            map.add(block);
        }
        return map;
    }

    public Set toSet(IQLParser.SetContext set) {

        String op = set.INTERSECT() != null ? set.INTERSECT().getText() :
                set.MINUS() != null ? set.MINUS().getText() :
                set.UNION() != null ? set.UNION().getText() :
                set.UNIONALL() != null ? set.UNIONALL().getText() : null;

        if (set.LEFT_PAREN() != null) {
            return toSet(set.set().get(0));
        } else if (set.select() != null) {
            Set bean = new Set();
            bean.setSelect(toSelect(set.select()));
            return bean;
        } else if (op != null) {
            Set bean = new Set();
            bean.setOperator(op);
            bean.setLeft(toSet(set.set().get(0)));
            bean.setRight(toSet(set.set().get(1)));
            return bean;
        } else {
            throw new RuntimeException();
        }
    }

    public Select toSelect(IQLParser.SelectContext select) {

        if (select.LEFT_PAREN() != null) {
            return toSelect(select.select());
        } else {
            Select bean = new Select();
            bean.setStart(toEntity(select.join_entity()));

            if (select.link() != null) {
                bean.setJoin(toJoin(select.link()));
            }
            if (select.filter() != null) {
                LogicalExpression n = Normalizer.normalize(toLogicalNode(select.filter()));
                bean.setFilter(n);
            }
            if (select.having() != null) {
                LogicalExpression n = Normalizer.normalize(toLogicalNode(select.having()));
                bean.setHaving(n);
            }
            return bean;
        }
    }

    public List<Join> toJoin(IQLParser.LinkContext link) {

        List<Join> list = new ArrayList<>();
        for (IQLParser.JoinContext j : link.join()) {
            list.add(toJoin(j));
        }

        return list ;
    }

    public Join toJoin(IQLParser.JoinContext join) {

        Join bean = new Join();
        bean.setCrit(join.crit.getText());
        if (join.ref != null) {
            bean.setRef(join.ref.getText());
        }
        bean.setOptional(join.OPTIONAL() != null);
        bean.setInvers(join.INVERS() != null);
        bean.setTable(toEntity(join.join_entity()));

        if (join.link() != null) {
            bean.setJoin(toJoin(join.link()));
        }

        return bean;
    }

    public Exists toExists(IQLParser.ExistsContext exists) {

        Exists bean = new Exists();
        bean.setCrit(exists.crit.getText());
        bean.setInvers(exists.INVERS() != null);
        bean.setTable(toEntity(exists.exists_entity()));
        if (exists.alias != null) {
            bean.setAlias(exists.alias.getText());
        }

        if (exists.link() != null) {
            bean.setJoin(toJoin(exists.link()));
        }
        return bean;
    }

    public Table toEntity(IQLParser.Join_entityContext entity) {

        Table table = new Table();
        table.setName(entity.table().tab.getText());
        if (entity.table().alias != null) {
            table.setAlias(entity.table().alias.getText());
        }

        for (IQLParser.OutContext o : entity.out()) {
            table.getOut().add(toOut(o));
        }
        if (entity.filter() != null) {
            LogicalExpression n = Normalizer.normalize(toLogicalNode(entity.filter()));
            table.setFilter(n);
        }
        for (IQLParser.OrderContext o : entity.order()) {
            table.getOrder().add(toOrder(o));
        }
        for (IQLParser.GroupContext g : entity.group()) {
            table.getGroup().add(toGroup(g));
        }
        if (entity.having() != null) {
            LogicalExpression n = Normalizer.normalize(toLogicalNode(entity.having()));
            table.setHaving(n);
        }

        return table;
    }

    public Table toEntity(IQLParser.Exists_entityContext entity) {

        Table table = new Table();

        table.setName(entity.table().tab.getText());
        if (entity.table().alias != null) {
            table.setAlias(entity.table().alias.getText());
        }

        LogicalExpression f = Normalizer.normalize(toLogicalNode(entity.filter()));
        table.setFilter(f);
        for (IQLParser.OrderContext o : entity.order()) {
            table.getOrder().add(toOrder(o));
        }
        for (IQLParser.GroupContext g : entity.group()) {
            table.getGroup().add(toGroup(g));
        }
        LogicalExpression h = Normalizer.normalize(toLogicalNode(entity.having()));
        table.setHaving(h);

        return table;
    }

    public Out toOut(IQLParser.OutContext out) {

        Out bean = new Out();
        if (out.h != null) {
            bean.setHeader(out.h.getText());
        }
        bean.setExpression(toExpression(out.expression()));

        if (out.idx != null) {
            bean.setIdx(Integer.valueOf(out.idx.getText()));
        }
        return bean;
    }

    public Expression toExpression(IQLParser.ExpressionContext expression) {

        if (expression.LEFT_PAREN() != null) {
            if (expression.expression() != null) {
                return toExpression(expression.expression());
            } else if (expression.set() != null) {
                Expression bean = new Expression();
                bean.setSet(toSet(expression.set()));
                return bean;
            } else {
                throw new RuntimeException();
            }
        } else if (expression.date_literal() != null) {
            return toExpression(expression.date_literal());
        } else if (expression.NUMBER() != null) {
            Expression bean = new Expression();
            bean.setNumber(Double.valueOf(expression.NUMBER().getText()));
            return bean;
        } else if (expression.SQ_STRING() != null) {
            Expression bean = new Expression();
            bean.setText(expression.SQ_STRING().getText());
            return bean;
        } else if (expression.column() != null) {
            return toExpression(expression.column());
        } else if (expression.function() != null) {
            return toExpression(expression.function());
        } else {
            throw new RuntimeException();
        }
    }

    public Expression toExpression(IQLParser.ColumnContext column) {
        Expression bean = new Expression();

        Column c = new Column();
        if (column.alias != null) {
            c.setAlias(column.alias.getText());
        }
        c.setCol(column.col.getText());
        bean.setColumn(c);
        return bean;
    }

    public Expression toExpression(IQLParser.FunctionContext function) {
        Expression bean = new Expression();

        Function f = new Function();
        f.setFunc(function.ID().getText());

        for (IQLParser.ArgumentContext a : function.argument()) {
            f.getArguments().add(toExpression(a));
        }
        bean.setFunction(f);
        return bean;
    }

    public Expression toExpression(IQLParser.ArgumentContext argument) {
         if (argument.expression() != null) {
             return toExpression(argument.expression());
         } else {
             Expression e = new Expression();
             e.setIdentity(argument.identity.getText());
             return e;
         }
    }

    public Expression toExpression(IQLParser.Date_literalContext date) {
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

    public LogicalExpression toLogicalNode(IQLParser.FilterContext filter) {
        if (filter == null) {
            return null;
        }

        return toLogicalNode(filter.logical_expression());
    }

    public LogicalExpression toLogicalNode(IQLParser.HavingContext having) {
        if (having == null) {
            return null;
        }

        return toLogicalNode(having.logical_expression());
    }


    public LogicalExpression toLogicalNode(IQLParser.Logical_expressionContext logicalExpression) {

        if (logicalExpression.unary_logical_expression() != null) {
            return LogicalExpression.value(toUnaryLogicalExpression(logicalExpression.unary_logical_expression()));
        } else if (logicalExpression.NOT() != null) {
            LogicalExpression n = toLogicalNode(logicalExpression.negate);
            return LogicalExpression.not(n);
        } else {
            LogicalExpression left = toLogicalNode(logicalExpression.left);
            LogicalExpression right = toLogicalNode(logicalExpression.right);
            return logicalExpression.AND() != null ? LogicalExpression.and(left, right) :  LogicalExpression.or(left, right);
        }
    }

    public static boolean isAnd(ParseTree tree) {
        return tree instanceof TerminalNode && tree.getText().equalsIgnoreCase("AND");
    }

    public UnaryLogicalExpression toUnaryLogicalExpression(IQLParser.Unary_logical_expressionContext unaryLogicalExpressionContext) {

        if (unaryLogicalExpressionContext.logical_expression() != null) {
            UnaryLogicalExpression bean = new UnaryLogicalExpression();
            LogicalExpression f = Normalizer.normalize(toLogicalNode(unaryLogicalExpressionContext.logical_expression()));
            bean.setNode(f);
            return bean;
        } else if (unaryLogicalExpressionContext.operator() != null) {
            UnaryLogicalExpression bean = new UnaryLogicalExpression();
            //bean.setNot(unaryLogicalExpressionContext.NOT() != null);
            bean.setOp(unaryLogicalExpressionContext.operator().getText());
            bean.setLeft(toExpression(unaryLogicalExpressionContext.expression().get(0)));
            for (int i = 1; i < unaryLogicalExpressionContext.expression().size(); i++) {
                bean.getRight().add(toExpression(unaryLogicalExpressionContext.expression().get(i)));
            }
            return bean;
        } if (unaryLogicalExpressionContext.exists() != null) {
            UnaryLogicalExpression bean = new UnaryLogicalExpression();
            //bean.setNot(unaryLogicalExpressionContext.NOT() != null);
            if (unaryLogicalExpressionContext.parent != null) {
                bean.setParent(unaryLogicalExpressionContext.parent.getText());
            }
            bean.setExists(toExists(unaryLogicalExpressionContext.exists()));
            return bean;
        } else {
            throw new RuntimeException();
        }
    }

    public Order toOrder(IQLParser.OrderContext order) {
        Order bean = new Order();
        if (order.expression() != null) {
            bean.setExpression(toExpression(order.expression()));
        } else if (order.header() != null) {
            bean.setHeader(order.header().getText());
        }
        bean.setAsc(order.DESC() != null);
        return bean;
    }

    public Group toGroup(IQLParser.GroupContext group) {
        Group bean = new Group();
        bean.setExpression(toExpression(group.expression()));
        return bean;
    }
}
