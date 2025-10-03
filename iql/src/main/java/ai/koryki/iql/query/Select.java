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
package ai.koryki.iql.query;

import java.util.ArrayList;
import java.util.List;

public class Select {

    private Table start;
    private List<Join> join = new ArrayList<>();

    public List<Join> getJoin() {
        return join;
    }

    public void setJoin(List<Join> join) {
        this.join = join;
    }

    private LogicalExpression filter;
    private LogicalExpression having;
    private int limit;

    public Table getStart() {
        return start;
    }

    public void setStart(Table start) {
        this.start = start;
    }

    public LogicalExpression getFilter() {
        return filter;
    }

    public void setFilter(LogicalExpression filter) {
        this.filter = filter;
    }

    public LogicalExpression getHaving() {
        return having;
    }

    public void setHaving(LogicalExpression having) {
        this.having = having;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
