package ai.koryki.iql.rules;

import ai.koryki.iql.DefaultVisitor;
import ai.koryki.iql.Visitor;
import ai.koryki.iql.Walker;
import ai.koryki.iql.logic.NodeType;
import ai.koryki.iql.query.*;
import ai.koryki.model.schema.Schema;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Push logical expressions from select.filter, to table.filter.
 * We must do this to execute outer joins correctly, otherwise
 * they will behave like inner joins.
 */
public class PushLogicalExpressionRule {

    private final Schema db;

    public PushLogicalExpressionRule(Schema db) {
        this.db = db;
    }

    public void apply(Query query) {

        PushExpressionVisitor v = new PushExpressionVisitor(db);
        new Walker().walk(query, v);
    }

    private static class PushExpressionVisitor extends DefaultVisitor {

        private final Schema db;

        public PushExpressionVisitor(Schema db) {
            this.db = db;
        }

        @Override
        public void visit(Deque<Object> deque, Select select) {

            LogicalExpression filter = select.getFilter();
            if (filter != null) {

                if (filter.getType().isValue()) {
                    String a = homogenAlias(filter);
                    if (a != null) {
                        // push filter at all

                        Table table = Visitor.table(select, a);
                        table.setFilter(LogicalExpression.and(filter, table.getFilter()));
                        select.setFilter(null);
                    }
                } else if (filter.getType().isNot()) {
                    // do not push expression
                } else if (filter.getType().equals(NodeType.OR)) {
                    // do not push expression
                } else {
                    // we have AND-Filter
                    List<LogicalExpression> children = new ArrayList<>(filter.getChildren());
                    for (LogicalExpression c : children) {
                        String a = homogenAlias(c);
                        if (a != null) {
                            // push children
                            Table table = Visitor.table(select, a);
                            table.setFilter(LogicalExpression.and(c, table.getFilter()));
                            // remove from filter
                            filter.getChildren().remove(c);
                        }
                    }
                }
            }
        }
    }

    private static String homogenAlias(LogicalExpression logicalExpression) {
        if (logicalExpression.getUnaryRelationalExpression() != null) {
            return homogenAlias(logicalExpression.getUnaryRelationalExpression());
        } else {
            List<String> aliases =
                    logicalExpression.getChildren().stream()
                            .map(PushLogicalExpressionRule::homogenAlias)
                            .distinct()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
            return aliases.size() == 1 ? aliases.get(0) : null;
        }
    }

    private static String homogenAlias(UnaryLogicalExpression u) {

        if (u.getNode() != null) {
            return null;
        } else {

            List<Expression> expressions = new ArrayList<>();
            expressions.add(u.getLeft());
            expressions.addAll(u.getRight());

            List<String> aliases =
                    expressions.stream()
                            .map(PushLogicalExpressionRule::homogenAlias)
                            .distinct()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
            return aliases.size() == 1 ? aliases.get(0) : null;
        }
    }

    private static String homogenAlias(Expression expression) {

        if (expression.getIdentity() != null) {
            return expression.getIdentity();
        } else if (expression.getColumn() != null) {
            return expression.getColumn().getAlias();
        } else if (expression.getFunction() != null) {

            List<String> aliases = expression.getFunction().getArguments().stream()
                    .map(PushLogicalExpressionRule::homogenAlias)
                    .distinct()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return aliases.size() == 1 ? aliases.get(0) : null;
        } else if (expression.getLeft() != null) {

            String l = homogenAlias(expression.getLeft());
            String r = null;
            if (expression.getRight() != null) {
                r = homogenAlias(expression.getRight());
            }
            return r == null || r.equals(l) ? l : null;
        }
        return null;
    }
}
