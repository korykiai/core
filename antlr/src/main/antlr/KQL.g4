/*
 * Copyright 2025 Johannes Zemlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

grammar KQL;

query
 : (WITH (block) (COMMA block)*)? set EOF
;

block : ID AS LEFT_PAREN set RIGHT_PAREN
;

set : set SET_INTERSECT set
| set (SET_UNION | SET_UNIONALL | SET_MINUS) set
| LEFT_PAREN set RIGHT_PAREN
| select
;

select
 : FIND table (COMMA link)* filterClause? fetchClause? (ORDER (order) (COMMA order)*)? limitClause?
;

link
 : from=ID backward=LESS? BAR forward=GREATER? to=ID alias=ID
| from=ID backward=LESS? PLUS forward=GREATER? to=ID alias=ID
| from=ID backward=LESS? BAR crit=ID BAR forward=GREATER? to=ID alias=ID
| from=ID backward=LESS? BAR crit=ID PLUS forward=GREATER? to=ID alias=ID
;

table
 : name=ID alias=ID
;

filterClause
 : FILTER logical_expression
;

order : (expression | header) (ASC | DESC)?;

limitClause
 : LIMIT NUMBER
;

logical_expression
 : NOT negate=logical_expression
| left=logical_expression AND right=logical_expression
| left=logical_expression OR right=logical_expression
| unary_logical_expression
;

unary_logical_expression
 : expression operator
 ( expression?
| (expression AND expression)
| LEFT_PAREN expression (COMMA expression)* RIGHT_PAREN
 )
| LEFT_PAREN logical_expression RIGHT_PAREN
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

fetchClause
 : FETCH fetchItem (COMMA fetchItem)*
;

fetchItem
 : expression (h=ID)?
;

expression
 : LEFT_PAREN expression RIGHT_PAREN
| left=expression (MULT | DIV) right=expression
| left=expression (PLUS | MINUS) right=expression
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
 : func=ID LEFT_PAREN (argument (COMMA argument)*)? RIGHT_PAREN
;

argument
 : expression | identity=ID
;

column
 : alias=ID DOT col=ID
;

header
 : h=ID
;

WITH : 'WITH';
AS : 'AS';
ORDER : 'ORDER';
LIMIT : 'LIMIT';
DESC : 'DESC';
ASC : 'ASC';
FIND : 'FIND';
FILTER : 'FILTER';
FETCH : 'FETCH';
AND : 'AND';
OR : 'OR';
NOT : 'NOT';
DATE : 'DATE';
TIME : 'TIME';
TIMESTAMP : 'TIMESTAMP';
SET_UNION : 'UNION';
SET_UNIONALL : 'UNIONALL';
SET_MINUS : 'MINUS';
SET_INTERSECT : 'INTERSECT';

DATE_FORMAT
 : SINGLE_QUOTE YEAR '-' MONTH '-' DAY SINGLE_QUOTE // 'YYYY-MM-DD'
;

TIME_FORMAT
 : SINGLE_QUOTE HOUR ':' MINUTE ':' SECOND ( '.' DIGIT DIGIT DIGIT)? (('+'|'-') TZ)? SINGLE_QUOTE // 'HH:MI:SS'
;

TIMESTAMP_FORMAT
 : SINGLE_QUOTE YEAR '-' MONTH '-' DAY ' ' HOUR ':' MINUTE ':' SECOND ( '.' DIGIT DIGIT DIGIT)? (('+'|'-') TZ)? SINGLE_QUOTE // 'YYYY-MM-DD HH:MI:SS'
;

TZ
 : HOUR ':' MINUTE
;


fragment DIGIT
 : [0-9]
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

STRING
: '"' ('\\"' | .)*? '"'
;

SQ_STRING
: SINGLE_QUOTE ('\\\'' | .)*? SINGLE_QUOTE
;

fragment LETTER
: [a-zA-Z\u0080-\u00FF_]
;

fragment LOWER
: [a-z\u0080-\u00FF_]
;

SINGLE_QUOTE: '\'';
DOT: '.';
BAR: '-';

EQUALS: '=';
COMMA: ',';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MINUS: BAR;
MULT: '*';
DIV: '/';
LEFT_PAREN: '(';
RIGHT_PAREN: ')';
LEFT_CURLY: '{';
RIGHT_CURLY: '}';

LEFT_BRACKET: '[';
RIGHT_BRACKET: ']';

LESS: '<';
GREATER: '>';
LESSEQ: '<=';
GREATEREQ: '>=';
LIKE: 'LIKE';
BETWEEN: 'BETWEEN';
ISNULL: 'ISNULL';
IN: 'IN';

ID
: LOWER (LOWER | DIGIT)*
;

fragment A: [aA];
fragment B: [bB];
fragment C: [cC];
fragment D: [dD];
fragment E: [eE];
fragment F: [fF];
fragment G: [gG];
fragment H: [hH];
fragment I: [iI];
fragment J: [jJ];
fragment K: [kK];
fragment L: [lL];
fragment M: [mM];
fragment N: [nN];
fragment O: [oO];
fragment P: [pP];
fragment Q: [qQ];
fragment R: [rR];
fragment S: [sS];
fragment T: [tT];
fragment U: [uU];
fragment V: [vV];
fragment W: [wW];
fragment X: [xX];
fragment Y: [yY];
fragment Z: [zZ];

COMMENT
: '/*' .*? '*/' -> channel(HIDDEN)
;

LINE_COMMENT
: '//' .*? '\r'? '\n' -> channel(HIDDEN)
;

WS
: [ \t\n\r]+ -> channel(HIDDEN)
;