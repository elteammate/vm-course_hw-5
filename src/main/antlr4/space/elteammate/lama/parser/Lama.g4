grammar Lama;

module : expr EOF;

primary
    : NUM # Number
    | IDENT '(' expr (',' expr)* ','? ')' # DirectCall
    | IDENT # Lookup
    | '(' expr ')' # Parenthesized
    ;

expr
    : primary (OP primary)* # RawExpr
    ;

IDENT : [A-Za-z_][A-Za-z0-9_]* ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> skip ;
OP : [+\-*/%<=>!&:]+ ;
