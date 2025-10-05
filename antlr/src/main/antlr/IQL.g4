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

grammar IQL;

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

// keywords are case-independent

QUERY
    : Q U E R Y
;

SELECT
    : S E L E C T
;

UNION
    : U N I O N
;

UNIONALL
    : U N I O N A L L
;

MINUS
    : M I N U S
;

INTERSECT
    : I N T E R S E C T
;

JOIN
    : J O I N
;

EXISTS
    : E X I S T S
;

OWNER
    : O W N E R
;

OPTIONAL
    : O P T I O N A L
;

INVERS
    : I N V E R S
;

ALL
    : A L L
;

NOT
    : N O T
;

OUT
    : O U T
;

GROUP
    : G R O U P
;

ORDER
    : O R D E R
;

ASC
    : A S C
;

DESC
    : D E S C
;

AS
    : A S
;

WITH
    : W I T H
;

FILTER
    : F I L T E R
;

MIXFILTER
    : M I X F I L T E R
;

HAVING
    : H A V I N G
;

MIXHAVING
    : M I X H A V I N G
;


NULL
    : N U L L
;

AND
    : A N D
;

OR
    : O R
;

DATE
    : D A T E
;

TIME
    : T I M E
;

TIMESTAMP
    :  T I M E S T A M P
;

DATE_FORMAT
    : SINGLE_QUOTE YEAR '-' MONTH '-' DAY SINGLE_QUOTE                           // 'YYYY-MM-DD'
    ;

TIME_FORMAT
    : SINGLE_QUOTE HOUR ':' MINUTE ':' SECOND ( '.' DIGIT DIGIT DIGIT)? (('+'|'-') OFFSET)? SINGLE_QUOTE                         // 'HH:MI:SS'
    ;

TIMESTAMP_FORMAT
    : SINGLE_QUOTE YEAR '-' MONTH '-' DAY ' ' HOUR ':' MINUTE ':' SECOND ( '.' DIGIT DIGIT DIGIT)? (('+'|'-') OFFSET)?  SINGLE_QUOTE                      // 'YYYY-MM-DD HH:MI:SS'
    ;

OFFSET
    : HOUR ':' MINUTE
;

fragment YEAR
    : DIGIT DIGIT DIGIT DIGIT
;

fragment MONTH
    : '0' [1-9]
    | '1' [0-2]
;

fragment DAY
    : '0' [1-9]
    | [12] DIGIT
    | '3' [01]
;

fragment HOUR
    : [01] DIGIT
    | '2' [0-3]
;

fragment MINUTE
    : [0-5] DIGIT
;

fragment SECOND
    : [0-5] DIGIT
;

NUMBER
    : '-'? ('.' DIGIT+ | DIGIT+ ( '.' DIGIT*)?)
;

fragment DIGIT
    : [0-9]
    ;

/** "any double-quoted string ("...") possibly containing escaped quotes" */
STRING
    : '"' ('\\"' | .)*? '"'
    ;

/** "any single-quoted string ('...') possibly containing escaped single-quotes" */
SQ_STRING
    : SINGLE_QUOTE ('\\\'' | .)*? SINGLE_QUOTE
    ;

fragment LETTER
    : [a-zA-Z\u0080-\u00FF_]
    ;

SINGLE_QUOTE:               '\'';
DOT :                       '.';
EQUALS:                     '=';
COMMA:                      ',';
SEMICOLON:                  ';';
COLON:                      ':';
LEFT_PAREN:                '(';
RIGHT_PAREN:               ')';
LEFT_CURLY:                '{';
RIGHT_CURLY:               '}';
PLUS_SIGN:                      '+';
MINUS_SIGN : '-';
MULT_SIGN:                      '*';
DIV_SIGN:                      '/';

LEFT_BRACKET:                '[';
RIGHT_BRACKET:               ']';

LESS:                   '<';
GREATER:                '>';
LESSEQ:                 '<=';
GREATEREQ:              '>=';
LIKE:                   L I K E ;
BETWEEN:                B E T W E E N;
ISNULL:                 I S N U L L;
IN:                     I N;

ID
    : LETTER (LETTER | DIGIT)*
    ;

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];

COMMENT
    : '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    : '//' .*? '\r'? '\n' -> channel(HIDDEN)
    ;

WS
    : [ \t\n\r]+ -> channel(HIDDEN)
;