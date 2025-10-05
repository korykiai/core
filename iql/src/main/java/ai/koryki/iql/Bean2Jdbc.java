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

import ai.koryki.iql.query.Expression;
import ai.koryki.iql.query.Query;

public class Bean2Jdbc extends Bean2Sql {

    public Bean2Jdbc(RelationResolver resolver, Query query) {
        super(resolver, query);
    }

    @Override
    protected String timeExpression(Expression expression) {
        return "{t '" + expression.getLocalTime() + "'}";
    }

    @Override
    protected String timestempExpression(Expression expression) {
        return "{ts '" + expression.getLocalDateTime() + "'}";
    }

    @Override
    protected String dateExpression(Expression expression) {

        return "{d '" + expression.getLocalDate() + "'}";
    }

}
