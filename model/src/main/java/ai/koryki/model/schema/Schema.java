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
package ai.koryki.model.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Schema implements Cloneable {

    private String name;
    private String comment;
    private String description;
    private List<Table> tables;
    private List<Relation> relations;

    public Schema() {
        this(null);
    }

    public Schema(String name)  {
        this(name, null, null);
    }

    public Schema(String name, String comment, String description) {
        this(name, comment, description, new ArrayList<>(), new ArrayList<>());
    }

    public Schema(String name, String comment, String description, List<Table> tables, List<Relation> relations)  {
        this.name = name;
        this.comment = comment;
        this.description = description;
        this.tables = new ArrayList<>(tables);
        this.relations = new ArrayList<>(relations);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = new ArrayList<>(tables);
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public void setRelations(List<Relation> relations) {
        this.relations = new ArrayList<>(relations);
    }

    public void addTable(Table table) {
        this.tables.add(table);
    }

    public void addRelation(Relation relation) {
        this.relations.add(relation);
    }

    public Optional<Table> getTable(String name) {
        return tables.stream().filter(t -> t.getName().equals(name)).findFirst();
    }

    public Optional<Relation> getRelation(String name) {
        return relations.stream().filter(r -> r.getName().equals(name)).findFirst();
    }

    @Override
    public Schema clone() {
            Schema d = new Schema(name, comment, description);
            tables.forEach(t -> d.tables.add(t.clone()));
            relations.forEach(t -> d.relations.add(t.clone()));
            return d;
    }

}
