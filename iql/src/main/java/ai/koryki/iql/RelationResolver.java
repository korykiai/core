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

import ai.koryki.model.schema.Schema;
import ai.koryki.model.schema.Relation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RelationResolver {

    private Schema db;
    private Map<String, List<String>> linkToRelations;
    private boolean alignedOnly;
    private boolean qualifiedOnly;
    private boolean strict;


    public RelationResolver(Schema db, Map<String, List<String>> linkToRelations) {
        this(db, linkToRelations, false, false);
    }

    public RelationResolver(Schema db, Map<String, List<String>> linkToRelations, boolean alignedOnly, boolean qualifiedOnly) {
        this.db = db;
        this.linkToRelations = linkToRelations;
        this.alignedOnly = alignedOnly;
        this.qualifiedOnly = qualifiedOnly;
    }

    private Predicate<Relation> typedAligned(String c, String s, String e) {
        return (r) ->  r.getName().equals(c) && r.getStartTable().equals(s) && r.getEndTable().equals(e);
    }
    private Predicate<Relation> typedReverse(String c, String s, String e) {
        return (r)  -> r.getName().equals(c) && r.getStartTable().equals(e) && r.getEndTable().equals(s);
    }
    private Predicate<Relation> untypedAligned(String c, String s, String e) {
        return strict ? untypedAlignedStrict(c, s, e) :  untypedAlignedLenient(c, s, e);
    }
    private Predicate<Relation> untypedReverse(String c, String s, String e) {
        return strict ? untypedReverseStrict(c, s, e) :  untypedReverseLenient(c, s, e);
    }

    private Predicate<Relation> untypedAlignedLenient(String c, String s, String e) {
        return (r) ->r.getStartTable().equals(s) && r.getEndTable().equals(e);
    }
    private Predicate<Relation> untypedReverseLenient(String c, String s, String e) {
        return (r) ->r.getStartTable().equals(e) && r.getEndTable().equals(s);
    }
    private Predicate<Relation> untypedAlignedStrict(String c, String s, String e) {
        return (r) ->c == null && r.getStartTable().equals(s) && r.getEndTable().equals(e);
    }
    private Predicate<Relation> untypedReverseStrict(String c, String s, String e) {
        return (r) ->c == null && r.getStartTable().equals(e) && r.getEndTable().equals(s);
    }

    public boolean isTableInDatabase(String table) {
        String s = Bean2Sql.strip(table);
        boolean r = db.getTables().stream().anyMatch(t -> t.getName().equals(s));
        return r;
    }

    private String resolveLink(String startTable, String endTable, String link) {

        if (link == null) {
            return null;
        }

        List<String> list = linkToRelations.get(link);

        if (list == null) {
            if (strict) {
                throw new RuntimeException("c'ant resolve link " + link);
            }
            return link;
        }

        List<Relation> relations = list.stream().map(l -> db.getRelation(l).get()).collect(Collectors.toList());

        String relation =  list.stream().filter(l -> {
                Relation r = db.getRelation(l).get();
                if (r.getStartTable().equals(startTable) && r.getEndTable().equals(endTable)) {
                    return true;
                } else if (r.isSymmetric() && r.getStartTable().equals(endTable) && r.getEndTable().equals(startTable)) {
                    return true;
                } else {
                    return false;
                }
            }).map(l -> db.getRelation(l).get().getName()).findFirst().orElse(null);

        if (strict && relation == null) {
            throw new RuntimeException("c'ant resolve link " + link + " " + startTable + " " + endTable);
        }
        return relation;
    }

    public Optional<Relation> find(String startTable, String endTable, String relation) {

        String s = Identifier.normal(Identifier.lowercase, startTable);
        String e = Identifier.normal(Identifier.lowercase, endTable);

        String c = resolveLink(startTable, endTable, relation);

        //String c = (relation == null || relation.trim().isEmpty()) ? null : Identifier.normal(Identifier.lowercase, relation);


        Relation d = checkSingle(s, e, c, typedAligned(c, s, e), true, false);
        if (d != null) {
            return Optional.of(d);
        }

        if (!alignedOnly) {
            d = checkSingle(s, e, c, typedReverse(c, s, e), true, true);
            if (d != null) {
                return Optional.of(d);
            }
        }

        if (!qualifiedOnly) {

            d = checkSingle(s, e, c, untypedAligned(c, s, e), false, false);
            if (d != null) {
                return Optional.of(d);
            }

            if (!alignedOnly) {
                d = checkSingle(s, e, c, untypedReverse(c, s, e), false, true);
                if (d != null) {
                    return Optional.of(d);
                }
            }
        }

        return Optional.empty();
    }

    private Relation checkSingle(String startTable, String endTable, String relation, Predicate<Relation> predicate, boolean strict, boolean symmetricOnly) {
        List<Relation> directed =
                db.getRelations().stream().filter(predicate)
                        .collect(Collectors.toList());

        Relation d = directed.size() == 1 ? directed.get(0) : null;
        if (d != null) {
            if (!symmetricOnly || isSymmetric(d)) {
                return d;
            }
        } else if (strict && directed.size() > 1) {
            throw new RuntimeException("must not find more than one relation: " + startTable +" " + endTable + " " + relation);
        }
        return null;
    }

    private boolean isSymmetric(Relation d) {
        // FIXME add symmetric check
        return true;
    }

    public boolean isAlignedOnly() {
        return alignedOnly;
    }

    public void setAlignedOnly(boolean alignedOnly) {
        this.alignedOnly = alignedOnly;
    }

    public boolean isQualifiedOnly() {
        return qualifiedOnly;
    }

    public void setQualifiedOnly(boolean qualifiedOnly) {
        this.qualifiedOnly = qualifiedOnly;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }


    public Schema getDb() {
        return db;
    }
}
