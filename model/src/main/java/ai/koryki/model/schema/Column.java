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

public class Column implements Cloneable {

    private String name;
    private String comment;
    private String description;
    private Type type;
    private Boolean nullable;
    private int pkPos;

    public Column() {

    }

    public Column(String name) {
        this.name = name;
    }

    public Column(String name, String comment, String description) {
        this.name = name;
        this.comment = comment;
        this.description = description;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public Column clone() {
        try {
            Column c = (Column)super.clone();
            c.type = type.clone();
            return c;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPkPos() {
        return pkPos;
    }

    public void setPkPos(int pkPos) {
        this.pkPos = pkPos;
    }
}
