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

parser grammar IQLParser;

options {
    tokenVocab=IQLLexer;
}

query
    : (cte)? set EOF
    ;

cte : WITH (STRING AS LEFT_PAREN set RIGHT_PAREN) (COMMA STRING AS LEFT_PAREN set RIGHT_PAREN)* ;


set : set INTERSECT set
    | set (UNION | UNIONALL | MINUS) set
    | LEFT_PAREN set RIGHT_PAREN
    | select
;

// place heterogeneous filters from inner joins here
// you are not allowed to reference tables form outer joins here
select
    : SELECT ((entity=join_entity)) link? (ALL filter? having?)?
    | LEFT_PAREN select RIGHT_PAREN
;

link
    : (JOIN join OWNER)+
;

// use local filters only here
// you are only allowed to reference the table itself, but subselects are allowed here too!
join_entity
    : table out* filter? group* having? order*
;

// use local filters only here
// you are only allowed to reference the table itself, but subselects are allowed here too!
exists_entity
    : table filter? group* having? order*
;

join
    : OPTIONAL? INVERS? crit=STRING (
          entity=join_entity // join an table
        | ref=STRING // join a to a references table or subquery
        | LEFT_PAREN set RIGHT_PAREN alias=STRING // join a subquery
        ) child=link?
;

exists
    : INVERS? crit=STRING (
          entity=exists_entity  // exists table
        | LEFT_PAREN set RIGHT_PAREN alias=STRING // exists subquery
        ) child=link?
;

expression
    : LEFT_PAREN expression RIGHT_PAREN
    | date_literal
    | column
    | function
    | NUMBER
    | SQ_STRING
    | LEFT_PAREN set RIGHT_PAREN
;

date_literal
    : DATE DATE_FORMAT
    | TIME TIME_FORMAT
    | TIMESTAMP TIMESTAMP_FORMAT
;

function
    : ID LEFT_PAREN (argument (COMMA argument)*)? RIGHT_PAREN
;

argument
    : expression | identity=ID
;

column
    : (alias=STRING DOT)? col=STRING
;

table
    :  tab=STRING (alias=STRING)?
;

out
    : OUT expression (h=STRING)? (idx=NUMBER)?
;

group
    : GROUP expression
;

order
    : ORDER (expression | header) (ASC | DESC)?
;

filter
    : FILTER logical_expression
;

having
    : HAVING logical_expression
;

logical_expression
    : NOT negate=logical_expression
    | left=logical_expression AND right=logical_expression
    | left=logical_expression OR right=logical_expression
    | unary_logical_expression
    ;

unary_logical_expression
    : expression operator
        ( expression? // nothing or single expression
        | (expression AND expression) // pair of expressions
        | LEFT_PAREN expression (COMMA expression)* RIGHT_PAREN // set of expressions
        )
    | LEFT_PAREN logical_expression RIGHT_PAREN
    | parent=STRING? EXISTS LEFT_PAREN exists RIGHT_PAREN
    ;

operator
    : BETWEEN
    | EQUALS
    | GREATER
    | GREATEREQ
    | IN
    | ISNULL
    | LESS
    | LESSEQ
    | LIKE
    | custom=STRING
;

header
    : h=ID
;
