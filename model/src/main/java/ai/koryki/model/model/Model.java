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

public class Model {

    private String name;
    private String comment;
    private String description;
    private List<Entity> entities;
    private List<Link> links;

    public Model() {
        this.entities = new ArrayList<>();
        this.links = new ArrayList<>();
    }


    public Model(String name)  {
        this.name = name;
        this.entities = new ArrayList<>();
        this.links = new ArrayList<>();
    }


    public Model(String name, String comment, String description, List<Entity> entities, List<Link> links)  {
        this.name = name;
        this.comment = comment;
        this.description = description;
        this.entities = new ArrayList<>(entities);
        this.links = new ArrayList<>(links);
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

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = new ArrayList<>(entities);
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = new ArrayList<>(links);
    }

    public void addEntity(Entity entity) {
        this.entities.add(entity);
    }

    public void addLink(Link link) {
        this.links.add(link);
    }

}
