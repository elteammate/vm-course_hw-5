grammar Lama;

module : definitions* expr EOF;

definitions
    : 'var' IDENT ('=' simpleExpr)? (',' IDENT ('=' simpleExpr)?)* ';' # VarDefinitions
    | 'fun' IDENT funParams funBody # FunDefinition
    ;

funParams
    : '(' ')' # EmptyParams
    | '(' IDENT (',' IDENT)* ')' # Params
    ;

funBody
    : '{' body=scoped '}'
    ;

scoped
    : definitions* expr # ScopedExpr
    ;

primary
    : NUM # Number
    | STRING # String
    | IDENT '(' (args+=expr (',' args+=expr)* ','?)? ')' # DirectCall
    | IDENT # Lookup
    | 'skip' # Skip
    | '(' scoped ')' # Parenthesized
    | 'while' definitions* cond=expr 'do' body=scoped 'od' # WhileDoLoop
    | 'do' definitions* body=expr 'while' cond=scoped 'od' # DoWhileLoop
    | 'if' ifStmtMiddle # IfStmt
    | 'for' definitions* init=expr ',' cond=expr ',' step=expr 'do' body=scoped 'od' # ForLoop
    | collection=primary '[' index=expr ']' # Indexing
    | recv=primary '.' IDENT ('(' (args+=expr (',' args+=expr)* ','?)? ')')? # DotCall
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
    | collection=primary '[' index=expr ']' # LIndexing
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
STRING : '"' ( ESC_SEQ | ~["\\] )* '"' ;
fragment ESC_SEQ
    : '\\' [nrt"\\]
    ;
