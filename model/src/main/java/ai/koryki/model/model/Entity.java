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
package ai.koryki.model.model;

import java.util.ArrayList;
import java.util.List;

public class Entity {

    private String name;
    private String comment;
    private String description;
    private List<Property> properties;

    public Entity()  {
        this.properties = new ArrayList<>();
    }

    public Entity(String name)  {
        this.name = name;
        this.properties = new ArrayList<>();
    }

    public Entity(String name, String comment, String description, List<Property> properties)  {
        this.name = name;
        this.comment = comment;
        this.description = description;
        this.properties = new ArrayList<>(properties);
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

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = new ArrayList<>(properties);
    }

    public void addProperty(Property property) {
        this.properties.add(property);
    }


}
