grammar Lama;

module : definitions* expr EOF;

definitions
    : 'var' IDENT ('=' simpleExpr)? (',' IDENT ('=' simpleExpr)?)* ';' # VarDefinitions
    ;

scoped
    : definitions* expr # ScopedExpr
    ;

primary
    : NUM # Number
    | IDENT '(' (args+=expr (',' args+=expr)* ','?)? ')' # DirectCall
    | IDENT # Lookup
    | 'skip' # Skip
    | '(' scoped ')' # Parenthesized
    | 'while' definitions* cond=expr 'do' body=scoped 'od' # WhileDoLoop
    | 'do' definitions* body=expr 'while' cond=scoped 'od' # DoWhileLoop
    | 'if' ifStmtMiddle # IfStmt
    | 'for' definitions* init=expr ',' cond=expr ',' step=expr 'do' body=scoped 'od' # ForLoop
    ;

ifStmtMiddle
    : definitions* cond=expr 'then' then=scoped ifRest
    ;

ifRest
    : 'fi' # IfRestEnd
    | 'else' else_=scoped 'fi' # IfElseEnd
    | 'elif' ifStmtMiddle # IfCont
    ;


simpleExpr
    : primary (OP primary)* # RawExpr
    ;

lvalue
    : IDENT # LLookup
    ;

expr
    : lvalue ':=' expr # Assignment
    | expr ';' expr # Seq
    | simpleExpr # Simple
    ;

IDENT : [A-Za-z_'][A-Za-z0-9_']* ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> skip ;
OP : [+\-*/%<=>!&:]+ ;
