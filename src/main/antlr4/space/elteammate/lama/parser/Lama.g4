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

lvalue
    : IDENT # LLookup
    ;

expr
    : lvalue ':=' expr # Assignment
    | expr ';' expr # Seq
    | primary (OP primary)* # RawExpr
    ;

IDENT : [A-Za-z_'][A-Za-z0-9_']* ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> skip ;
OP : [+\-*/%<=>!&:]+ ;
