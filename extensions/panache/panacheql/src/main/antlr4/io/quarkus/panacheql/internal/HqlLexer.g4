lexer grammar HqlLexer;


@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
}

WS : WS_CHAR+ -> skip;

fragment
WS_CHAR : [ \f\t\r\n];

COMMENT : '/*' (~'*' | '*' ~'/' )* '*/' -> skip;

fragment
DIGIT : [0-9];

fragment
HEX_DIGIT : [0-9a-fA-F];

fragment
EXPONENT : [eE] [+-]? DIGIT+;

fragment
LONG_SUFFIX : [lL];

fragment
FLOAT_SUFFIX : [fF];

fragment
DOUBLE_SUFFIX : [dD];

fragment
BIG_DECIMAL_SUFFIX : [bB] [dD];

fragment
BIG_INTEGER_SUFFIX : [bB] [iI];

// Although this is not 100% correct because this accepts leading zeros,
// we stick to this because temporal literals use this rule for simplicity.
// Since we don't support octal literals, this shouldn't really be a big issue
fragment
INTEGER_NUMBER
	: DIGIT+
	;

fragment
FLOATING_POINT_NUMBER
	: DIGIT+ '.' DIGIT* EXPONENT?
	| '.' DIGIT+ EXPONENT?
	| DIGIT+ EXPONENT
	| DIGIT+
	;

INTEGER_LITERAL : INTEGER_NUMBER ('_' INTEGER_NUMBER)*;

LONG_LITERAL : INTEGER_NUMBER  ('_' INTEGER_NUMBER)* LONG_SUFFIX;

FLOAT_LITERAL : FLOATING_POINT_NUMBER FLOAT_SUFFIX;

DOUBLE_LITERAL : FLOATING_POINT_NUMBER DOUBLE_SUFFIX?;

BIG_INTEGER_LITERAL : INTEGER_NUMBER BIG_INTEGER_SUFFIX;

BIG_DECIMAL_LITERAL : FLOATING_POINT_NUMBER BIG_DECIMAL_SUFFIX;

HEX_LITERAL : '0' [xX] HEX_DIGIT+ LONG_SUFFIX?;

fragment SINGLE_QUOTE : '\'';
fragment DOUBLE_QUOTE : '"';

STRING_LITERAL : SINGLE_QUOTE ( SINGLE_QUOTE SINGLE_QUOTE | ~('\'') )* SINGLE_QUOTE;

JAVA_STRING_LITERAL
	: DOUBLE_QUOTE ( ESCAPE_SEQUENCE | ~('"') )* DOUBLE_QUOTE
 	| [jJ] SINGLE_QUOTE ( ESCAPE_SEQUENCE | ~('\'') )* SINGLE_QUOTE
 	| [jJ] DOUBLE_QUOTE ( ESCAPE_SEQUENCE | ~('\'') )* DOUBLE_QUOTE
 	;

fragment BACKSLASH : '\\';

fragment
ESCAPE_SEQUENCE
	: BACKSLASH [btnfr"']
	| BACKSLASH UNICODE_ESCAPE
	| BACKSLASH BACKSLASH
	;

fragment
UNICODE_ESCAPE
	: 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
	;

BINARY_LITERAL
	: [xX] SINGLE_QUOTE (HEX_DIGIT HEX_DIGIT)* SINGLE_QUOTE
	| [xX] DOUBLE_QUOTE (HEX_DIGIT HEX_DIGIT)* DOUBLE_QUOTE
	;

// ESCAPE start tokens
TIMESTAMP_ESCAPE_START : '{ts';
DATE_ESCAPE_START : '{d';
TIME_ESCAPE_START : '{t';

EQUAL : '=';
NOT_EQUAL : '!=' | '^=' | '<>';
GREATER : '>';
GREATER_EQUAL : '>=';
LESS : '<';
LESS_EQUAL : '<=';

COMMA :	',';
DOT	: '.';
LEFT_PAREN : '(';
RIGHT_PAREN	: ')';
LEFT_BRACKET : '[';
RIGHT_BRACKET : ']';
LEFT_BRACE : '{';
RIGHT_BRACE : '}';
PLUS : '+';
MINUS :	'-';
ASTERISK : '*';
SLASH : '/';
PERCENT_OP	: '%';
AMPERSAND : '&';
SEMICOLON :	';';
COLON : ':';
PIPE : '|';
DOUBLE_PIPE : '||';
QUESTION_MARK :	'?';
ARROW :	'->';


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Keywords

ID 				: [iI][dD];
VERSION			: [vV] [eE] [rR] [sS] [iI] [oO] [nN];
VERSIONED		: [vV] [eE] [rR] [sS] [iI] [oO] [nN] [eE] [dD];
NATURALID		: [nN] [aA] [tT] [uU] [rR] [aA] [lL] [iI] [dD];
FK				: [fF] [kK];

ALL					: [aA] [lL] [lL];
AND					: [aA] [nN] [dD];
ANY					: [aA] [nN] [yY];
AS					: [aA] [sS];
ASC					: [aA] [sS] [cC];
AVG					: [aA] [vV] [gG];
BETWEEN	 			: [bB] [eE] [tT] [wW] [eE] [eE] [nN];
BOTH				: [bB] [oO] [tT] [hH];
BREADTH	 			: [bB] [rR] [eE] [aA] [dD] [tT] [hH];
BY					: [bB] [yY];
CASE				: [cC] [aA] [sS] [eE];
CAST				: [cC] [aA] [sS] [tT];
COLLATE				: [cC] [oO] [lL] [lL] [aA] [tT] [eE];
COUNT				: [cC] [oO] [uU] [nN] [tT];
CROSS				: [cC] [rR] [oO] [sS] [sS];
CUBE				: [cC] [uU] [bB] [eE];
CURRENT				: [cC] [uU] [rR] [rR] [eE] [nN] [tT];
CURRENT_DATE		: [cC] [uU] [rR] [rR] [eE] [nN] [tT] '_' [dD] [aA] [tT] [eE];
CURRENT_INSTANT		: [cC] [uU] [rR] [rR] [eE] [nN] [tT] '_' [iI] [nN] [sS] [tT] [aA] [nN] [tT]; //deprecated legacy
CURRENT_TIME		: [cC] [uU] [rR] [rR] [eE] [nN] [tT] '_' [tT] [iI] [mM] [eE];
CURRENT_TIMESTAMP	: [cC] [uU] [rR] [rR] [eE] [nN] [tT] '_' [tT] [iI] [mM] [eE] [sS] [tT] [aA] [mM] [pP];
CYCLE				: [cC] [yY] [cC] [lL] [eE];
DATE				: [dD] [aA] [tT] [eE];
DATETIME			: [dD] [aA] [tT] [eE] [tT] [iI] [mM] [eE];
DAY					: [dD] [aA] [yY];
DEFAULT				: [dD] [eE] [fF] [aA] [uU] [lL] [tT];
DELETE				: [dD] [eE] [lL] [eE] [tT] [eE];
DEPTH	 			: [dD] [eE] [pP] [tT] [hH];
DESC				: [dD] [eE] [sS] [cC];
DISTINCT			: [dD] [iI] [sS] [tT] [iI] [nN] [cC] [tT];
ELEMENT				: [eE] [lL] [eE] [mM] [eE] [nN] [tT];
ELEMENTS			: [eE] [lL] [eE] [mM] [eE] [nN] [tT] [sS];
ELSE				: [eE] [lL] [sS] [eE];
EMPTY				: [eE] [mM] [pP] [tT] [yY];
END					: [eE] [nN] [dD];
ENTRY				: [eE] [nN] [tT] [rR] [yY];
EPOCH				: [eE] [pP] [oO] [cC] [hH];
ERROR				: [eE] [rR] [rR] [oO] [rR];
ESCAPE				: [eE] [sS] [cC] [aA] [pP] [eE];
EVERY				: [eE] [vV] [eE] [rR] [yY];
EXCEPT				: [eE] [xX] [cC] [eE] [pP] [tT];
EXCLUDE				: [eE] [xX] [cC] [lL] [uU] [dD] [eE];
EXISTS				: [eE] [xX] [iI] [sS] [tT] [sS];
EXTRACT				: [eE] [xX] [tT] [rR] [aA] [cC] [tT];
FETCH				: [fF] [eE] [tT] [cC] [hH];
FILTER				: [fF] [iI] [lL] [tT] [eE] [rR];
FIRST				: [fF] [iI] [rR] [sS] [tT];
FOLLOWING			: [fF] [oO] [lL] [lL] [oO] [wW] [iI] [nN] [gG];
FOR					: [fF] [oO] [rR];
FORMAT				: [fF] [oO] [rR] [mM] [aA] [tT];
FROM				: [fF] [rR] [oO] [mM];
FULL				: [fF] [uU] [lL] [lL];
FUNCTION			: [fF] [uU] [nN] [cC] [tT] [iI] [oO] [nN];
GROUP				: [gG] [rR] [oO] [uU] [pP];
GROUPS				: [gG] [rR] [oO] [uU] [pP] [sS];
HAVING				: [hH] [aA] [vV] [iI] [nN] [gG];
HOUR				: [hH] [oO] [uU] [rR];
IGNORE				: [iI] [gG] [nN] [oO] [rR] [eE];
ILIKE				: [iI] [lL] [iI] [kK] [eE];
IN					: [iI] [nN];
INDEX				: [iI] [nN] [dD] [eE] [xX];
INDICES				: [iI] [nN] [dD] [iI] [cC] [eE] [sS];
INNER				: [iI] [nN] [nN] [eE] [rR];
INSERT				: [iI] [nN] [sS] [eE] [rR] [tT];
INSTANT				: [iI] [nN] [sS] [tT] [aA] [nN] [tT];
INTERSECT			: [iI] [nN] [tT] [eE] [rR] [sS] [eE] [cC] [tT];
INTO 				: [iI] [nN] [tT] [oO];
IS					: [iI] [sS];
JOIN				: [jJ] [oO] [iI] [nN];
KEY					: [kK] [eE] [yY];
KEYS				: [kK] [eE] [yY] [sS];
LAST				: [lL] [aA] [sS] [tT];
LATERAL				: [lL] [aA] [tT] [eE] [rR] [aA] [lL];
LEADING				: [lL] [eE] [aA] [dD] [iI] [nN] [gG];
LEFT				: [lL] [eE] [fF] [tT];
LIKE				: [lL] [iI] [kK] [eE];
LIMIT				: [lL] [iI] [mM] [iI] [tT];
LIST				: [lL] [iI] [sS] [tT];
LISTAGG				: [lL] [iI] [sS] [tT] [aA] [gG] [gG];
LOCAL				: [lL] [oO] [cC] [aA] [lL];
LOCAL_DATE			: [lL] [oO] [cC] [aA] [lL] '_' [dD] [aA] [tT] [eE];
LOCAL_DATETIME		: [lL] [oO] [cC] [aA] [lL] '_' [dD] [aA] [tT] [eE] [tT] [iI] [mM] [eE];
LOCAL_TIME			: [lL] [oO] [cC] [aA] [lL] '_' [tT] [iI] [mM] [eE];
MAP					: [mM] [aA] [pP];
MATERIALIZED		: [mM] [aA] [tT] [eE] [rR] [iI] [aA] [lL] [iI] [zZ] [eE] [dD];
MAX					: [mM] [aA] [xX];
MAXELEMENT			: [mM] [aA] [xX] [eE] [lL] [eE] [mM] [eE] [nN] [tT];
MAXINDEX			: [mM] [aA] [xX] [iI] [nN] [dD] [eE] [xX];
MEMBER				: [mM] [eE] [mM] [bB] [eE] [rR];
MICROSECOND			: [mM] [iI] [cC] [rR] [oO] [sS] [eE] [cC] [oO] [nN] [dD];
MILLISECOND			: [mM] [iI] [lL] [lL] [iI] [sS] [eE] [cC] [oO] [nN] [dD];
MIN					: [mM] [iI] [nN];
MINELEMENT			: [mM] [iI] [nN] [eE] [lL] [eE] [mM] [eE] [nN] [tT];
MININDEX			: [mM] [iI] [nN] [iI] [nN] [dD] [eE] [xX];
MINUTE				: [mM] [iI] [nN] [uU] [tT] [eE];
MONTH				: [mM] [oO] [nN] [tT] [hH];
NANOSECOND			: [nN] [aA] [nN] [oO] [sS] [eE] [cC] [oO] [nN] [dD];
NEW					: [nN] [eE] [wW];
NEXT				: [nN] [eE] [xX] [tT];
NO					: [nN] [oO];
NOT					: [nN] [oO] [tT];
NULLS				: [nN] [uU] [lL] [lL] [sS];
OBJECT				: [oO] [bB] [jJ] [eE] [cC] [tT];
OF					: [oO] [fF];
OFFSET				: [oO] [fF] [fF] [sS] [eE] [tT];
OFFSET_DATETIME		: [oO] [fF] [fF] [sS] [eE] [tT] '_' [dD] [aA] [tT] [eE] [tT] [iI] [mM] [eE];
ON					: [oO] [nN];
ONLY				: [oO] [nN] [lL] [yY];
OR					: [oO] [rR];
ORDER				: [oO] [rR] [dD] [eE] [rR];
OTHERS				: [oO] [tT] [hH] [eE] [rR] [sS];
OUTER				: [oO] [uU] [tT] [eE] [rR];
OVER				: [oO] [vV] [eE] [rR];
OVERFLOW			: [oO] [vV] [eE] [rR] [fF] [lL] [oO] [wW];
OVERLAY				: [oO] [vV] [eE] [rR] [lL] [aA] [yY];
PAD					: [pP] [aA] [dD];
PARTITION			: [pP] [aA] [rR] [tT] [iI] [tT] [iI] [oO] [nN];
PERCENT				: [pP] [eE] [rR] [cC] [eE] [nN] [tT];
PLACING				: [pP] [lL] [aA] [cC] [iI] [nN] [gG];
POSITION			: [pP] [oO] [sS] [iI] [tT] [iI] [oO] [nN];
PRECEDING			: [pP] [rR] [eE] [cC] [eE] [dD] [iI] [nN] [gG];
QUARTER				: [qQ] [uU] [aA] [rR] [tT] [eE] [rR];
RANGE				: [rR] [aA] [nN] [gG] [eE];
RESPECT				: [rR] [eE] [sS] [pP] [eE] [cC] [tT];
RIGHT				: [rR] [iI] [gG] [hH] [tT];
ROLLUP				: [rR] [oO] [lL] [lL] [uU] [pP];
ROW	    			: [rR] [oO] [wW];
ROWS    			: [rR] [oO] [wW] [sS];
SEARCH				: [sS] [eE] [aA] [rR] [cC] [hH];
SECOND				: [sS] [eE] [cC] [oO] [nN] [dD];
SELECT				: [sS] [eE] [lL] [eE] [cC] [tT];
SET					: [sS] [eE] [tT];
SIZE				: [sS] [iI] [zZ] [eE];
SOME				: [sS] [oO] [mM] [eE];
SUBSTRING			: [sS] [uU] [bB] [sS] [tT] [rR] [iI] [nN] [gG];
SUM					: [sS] [uM] [mM];
THEN				: [tT] [hH] [eE] [nN];
TIES				: [tT] [iI] [eE] [sS];
TIME				: [tT] [iI] [mM] [eE];
TIMESTAMP			: [tT] [iI] [mM] [eE] [sS] [tT] [aA] [mM] [pP];
TIMEZONE_HOUR		: [tT] [iI] [mM] [eE] [zZ] [oO] [nN] [eE] '_' [hH] [oO] [uU] [rR];
TIMEZONE_MINUTE		: [tT] [iI] [mM] [eE] [zZ] [oO] [nN] [eE] '_' [mM] [iI] [nN] [uU] [tT] [eE];
TO					: [tT] [oO];
TRAILING			: [tT] [rR] [aA] [iI] [lL] [iI] [nN] [gG];
TREAT				: [tT] [rR] [eE] [aA] [tT];
TRIM				: [tT] [rR] [iI] [mM];
TRUNC				: [tT] [rR] [uU] [nN] [cC];
TRUNCATE			: [tT] [rR] [uU] [nN] [cC] [aA] [tT] [eE];
TYPE				: [tT] [yY] [pP] [eE];
UNBOUNDED			: [uU] [nN] [bB] [oO] [uU] [nN] [dD] [eE] [dD];
UNION				: [uU] [nN] [iI] [oO] [nN];
UPDATE				: [uU] [pP] [dD] [aA] [tT] [eE];
USING				: [uU] [sS] [iI] [nN] [gG];
VALUE				: [vV] [aA] [lL] [uU] [eE];
VALUES				: [vV] [aA] [lL] [uU] [eE] [sS];
WEEK				: [wW] [eE] [eE] [kK];
WHEN				: [wW] [hH] [eE] [nN];
WHERE				: [wW] [hH] [eE] [rR] [eE];
WITH				: [wW] [iI] [tT] [hH];
WITHIN				: [wW] [iI] [tT] [hH] [iI] [nN];
WITHOUT				: [wW] [iI] [tT] [hH] [oO] [uU] [tT];
YEAR				: [yY] [eE] [aA] [rR];
ZONED				: [zZ] [oO] [nN] [eE] [dD];

// case-insensitive true, false and null recognition (split vote :)
TRUE 	: [tT] [rR] [uU] [eE];
FALSE 	: [fF] [aA] [lL] [sS] [eE];
NULL 	: [nN] [uU] [lL] [lL];


fragment
LETTER : [a-zA-Z\u0080-\ufffe_$];

// Identifiers
IDENTIFIER
	: LETTER (LETTER | DIGIT)*
	;

fragment
BACKTICK : '`';

QUOTED_IDENTIFIER
	: BACKTICK ( ESCAPE_SEQUENCE | '\\' BACKTICK | ~([`]) )* BACKTICK
	;
