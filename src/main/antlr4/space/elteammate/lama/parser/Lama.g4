grammar Lama;

module : definitions* expr EOF;

definitions
    : 'var' IDENT ('=' expr)? (',' IDENT ('=' expr)?)* ';' # VarDefinitions
    ;

primary
    : NUM # Number
    | IDENT '(' (args+=expr (',' args+=expr)* ','?)? ')' # DirectCall
    | IDENT # Lookup
    | '(' expr ')' # Parenthesized
    ;

expr
    : primary (OP primary)* # RawExpr
    | expr ';' expr # Seq
    ;

IDENT : [A-Za-z_'][A-Za-z0-9_']* ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> skip ;
OP : [+\-*/%<=>!&:]+ ;
