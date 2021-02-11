lexer grammar HqlLexer;


@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
}

WS : ( ' ' | '\t' | '\f' | EOL ) -> skip;

fragment
EOL	: [\r\n]+;

INTEGER_LITERAL : INTEGER_NUMBER ;

fragment
INTEGER_NUMBER : ('0' | '1'..'9' '0'..'9'*) ;

LONG_LITERAL : INTEGER_NUMBER ('l'|'L');

BIG_INTEGER_LITERAL : INTEGER_NUMBER ('bi'|'BI') ;

HEX_LITERAL : '0' ('x'|'X') HEX_DIGIT+ ('l'|'L')? ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

OCTAL_LITERAL : '0' ('0'..'7')+ ('l'|'L')? ;

FLOAT_LITERAL : FLOATING_POINT_NUMBER ('f'|'F')? ;

fragment
FLOATING_POINT_NUMBER
	: ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
	| '.' ('0'..'9')+ EXPONENT?
    | ('0'..'9')+ EXPONENT
    | ('0'..'9')+
	;

DOUBLE_LITERAL : FLOATING_POINT_NUMBER ('d'|'D') ;

BIG_DECIMAL_LITERAL : FLOATING_POINT_NUMBER ('bd'|'BD') ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

CHARACTER_LITERAL
	:	'\'' ( ESCAPE_SEQUENCE | ~('\''|'\\') ) '\'' {setText(getText().substring(1, getText().length()-1));}
	;

STRING_LITERAL
	:	'"' ( ESCAPE_SEQUENCE | ~('\\'|'"') )* '"' {setText(getText().substring(1, getText().length()-1));}
	|	('\'' ( ESCAPE_SEQUENCE | ~('\\'|'\'') )* '\'')+ {setText(getText().substring(1, getText().length()-1).replace("''", "'"));}
	;

fragment
ESCAPE_SEQUENCE
	:	'\\' ('b'|'t'|'n'|'f'|'r'|'\\"'|'\''|'\\')
	|	UNICODE_ESCAPE
	|	OCTAL_ESCAPE
	;

fragment
OCTAL_ESCAPE
	:	'\\' ('0'..'3') ('0'..'7') ('0'..'7')
	|	'\\' ('0'..'7') ('0'..'7')
	|	'\\' ('0'..'7')
	;

fragment
UNICODE_ESCAPE
	:	'\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
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
PERCENT	: '%';
AMPERSAND : '&';
SEMICOLON :	';';
COLON : ':';
PIPE : '|';
DOUBLE_PIPE : '||';
QUESTION_MARK :	'?';
ARROW :	'->';

// Keywords
ABS					: [aA] [bB] [sS];
AS					: [aA] [sS];
ALL					: [aA] [lL] [lL];
AND					: [aA] [nN] [dD];
ANY					: [aA] [nN] [yY];
ASC					: [aA] [sS] [cC];
AVG					: [aA] [vV] [gG];
BY					: [bB] [yY];
BETWEEN     		: [bB] [eE] [tT] [wW] [eE] [eE] [nN];
BIT_LENGTH  		: [bB] [iI] [tT] [_] [lL] [eE] [nN] [gG] [tT] [hH];
BOTH        		: [bB] [oO] [tT] [hH];
CASE        		: [cC] [aA] [sS] [eE];
CAST        		: [cC] [aA] [sS] [tT];
CHARACTER_LENGTH	: [cC] [hH] [aA] [rR] [aA] [cC] [tT] [eE] [rR] '_' [lL] [eE] [nN] [gG] [tT] [hH];
CLASS				: [cC] [lL] [aA] [sS] [sS];
COALESCE			: [cC] [oO] [aA] [lL] [eE] [sS] [cC] [eE];
COLLATE				: [cC] [oO] [lL] [lL] [aA] [tT] [eE];
CONCAT				: [cC] [oO] [nN] [cC] [aA] [tT];
COUNT				: [cC] [oO] [uU] [nN] [tT];
CURRENT_DATE		: [cC] [uU] [rR] [rR] [eE] [nN] [tT] '_' [dD] [aA] [tT] [eE];
CURRENT_TIME		: [cC] [uU] [rR] [rR] [eE] [nN] [tT] '_' [tT] [iI] [mM] [eE];
CURRENT_TIMESTAMP	: [cC] [uU] [rR] [rR] [eE] [nN] [tT] '_' [tT] [iI] [mM] [eE] [sS] [tT] [aA] [mM] [pP];
CROSS				: [cC] [rR] [oO] [sS] [sS];
DAY					: [dD] [aA] [yY];
DELETE				: [dD] [eE] [lL] [eE] [tT] [eE];
DESC				: [dD] [eE] [sS] [cC];
DISTINCT			: [dD] [iI] [sS] [tT] [iI] [nN] [cC] [tT];
ELEMENTS			: [eE] [lL] [eE] [mM] [eE] [nN] [tT] [sS];
ELSE				: [eE] [lL] [sS] [eE];
EMPTY				: [eE] [mM] [pP] [tT] [yY];
END					: [eE] [nN] [dD];
ENTRY				: [eE] [nN] [tT] [rR] [yY];
ESCAPE				: [eE] [sS] [cC] [aA] [pP] [eE];
EXISTS				: [eE] [xX] [iI] [sS] [tT] [sS];
EXTRACT				: [eE] [xX] [tT] [rR] [aA] [cC] [tT];
FETCH				: [fF] [eE] [tT] [cC] [hH];
FROM				: [fF] [rR] [oO] [mM];
FULL				: [fF] [uU] [lL] [lL];
FUNCTION			: [fF] [uU] [nN] [cC] [tT] [iI] [oO] [nN];
GROUP				: [gG] [rR] [oO] [uU] [pP];
HAVING				: [hH] [aA] [vV] [iI] [nN] [gG];
HOUR				: [hH] [oO] [uU] [rR];
IN					: [iI] [nN];
INDEX				: [iI] [nN] [dD] [eE] [xX];
INNER				: [iI] [nN] [nN] [eE] [rR];
INSERT				: [iI] [nN] [sS] [eE] [rR] [tT];
INTO 				: [iI] [nN] [tT] [oO];
IS					: [iI] [sS];
JOIN				: [jJ] [oO] [iI] [nN];
KEY					: [kK] [eE] [yY];
LEADING				: [lL] [eE] [aA] [dD] [iI] [nN] [gG];
LEFT				: [lL] [eE] [fF] [tT];
LENGTH				: [lL] [eE] [nN] [gG] [tT] [hH];
LIMIT				: [lL] [iI] [mM] [iI] [tT];
LIKE				: [lL] [iI] [kK] [eE];
LIST				: [lL] [iI] [sS] [tT];
LOCATE				: [lL] [oO] [cC] [aA] [tT] [eE];
LOWER				: [lL] [oO] [wW] [eE] [rR];
MAP					: [mM] [aA] [pP];
MAX					: [mM] [aA] [xX];
MAXELEMENT			: [mM] [aA] [xX] [eE] [lL] [eE] [mM] [eE] [nN] [tT];
MAXINDEX			: [mM] [aA] [xX] [iI] [nN] [dD] [eE] [xX];
MEMBER				: [mM] [eE] [mM] [bB] [eE] [rR];
MIN					: [mM] [iI] [nN];
MINELEMENT			: [mM] [iI] [nN] [eE] [lL] [eE] [mM] [eE] [nN] [tT];
MININDEX			: [mM] [iI] [nN] [iI] [nN] [dD] [eE] [xX];
MINUTE				: [mM] [iI] [nN] [uU] [tT] [eE];
MOD					: [mM] [oO] [dD];
MONTH				: [mM] [oO] [nN] [tT] [hH];
NEW					: [nN] [eE] [wW];
NOT					: [nN] [oO] [tT];
NULLIF				: [nN] [uU] [lL] [lL] [iI] [fF];
OBJECT				: [oO] [bB] [jJ] [eE] [cC] [tT];
OCTET_LENGTH		: [oO] [cC] [tT] [eE] [tT] '_' [lL] [eE] [nN] [gG] [tT] [hH];
OF					: [oO] [fF];
OFFSET				: [oO] [fF] [fF] [sS] [eE] [tT];
ON					: [oO] [nN];
OR					: [oO] [rR];
ORDER				: [oO] [rR] [dD] [eE] [rR];
OUTER				: [oO] [uU] [tT] [eE] [rR];
POSITION			: [pP] [oO] [sS] [iI] [tT] [iI] [oO] [nN];
RIGHT				: [rR] [iI] [gG] [hH] [tT];
SECOND				: [sS] [eE] [cC] [oO] [nN] [dD];
SELECT				: [sS] [eE] [lL] [eE] [cC] [tT];
SET					: [sS] [eE] [tT];
SIZE				: [sS] [iI] [zZ] [eE];
SQRT				: [sS] [qQ] [rR] [tT];
STR                 : [sS] [tT] [rR];
SUBSTRING			: [sS] [uU] [bB] [sS] [tT] [rR] [iI] [nN] [gG];
SUBSTR				: [sS] [uU] [bB] [sS] [tT] [rR];
SUM					: [sS] [uU] [mM];
THEN				: [tT] [hH] [eE] [nN];
TIMEZONE_HOUR		: [tT] [iI] [mM] [eE] [zZ] [oO] [nN] [eE] '_' [hH] [oO] [uU] [rR];
TIMEZONE_MINUTE		: [tT] [iI] [mM] [eE] [zZ] [oO] [nN] [eE] '_' [mM] [iI] [nN] [uU] [tT] [eE];
TRAILING			: [tT] [rR] [aA] [iI] [lL] [iI] [nN] [gG];
TREAT				: [tT] [rR] [eE] [aA] [tT];
TRIM				: [tT] [rR] [iI] [mM];
TYPE				: [tT] [yY] [pP] [eE];
UPDATE				: [uU] [pP] [dD] [aA] [tT] [eE];
UPPER				: [uU] [pP] [pP] [eE] [rR];
VALUE				: [vV] [aA] [lL] [uU] [eE];
WHEN				: [wW] [hH] [eE] [nN];
WHERE				: [wW] [hH] [eE] [rR] [eE];
WITH				: [wW] [iI] [tT] [hH];
YEAR				: [yY] [eE] [aA] [rR];

// case-insensitive true, false and null recognition (split vote :)
TRUE 	: [tT] [rR] [uU] [eE];
FALSE 	: [fF] [aA] [lL] [sS] [eE];
NULL 	: [nN] [uU] [lL] [lL];

// Identifiers
IDENTIFIER
	:	('a'..'z'|'A'..'Z'|'_'|'$'|'\u0080'..'\ufffe')('a'..'z'|'A'..'Z'|'_'|'$'|'0'..'9'|'\u0080'..'\ufffe')*
	;

QUOTED_IDENTIFIER
	: '`' ( ESCAPE_SEQUENCE | ~('\\'|'`') )* '`'
	;
