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
    | CHAR # Char
    | IDENT '(' (args+=expr (',' args+=expr)* ','?)? ')' # DirectCall
    | IDENT # Lookup
    | 'skip' # Skip
    | '(' scoped ')' # Parenthesized
    | 'while' definitions* cond=expr 'do' body=scoped 'od' # WhileDoLoop
    | 'do' definitions* body=expr 'while' cond=scoped 'od' # DoWhileLoop
    | 'if' ifStmtMiddle # IfStmt
    | 'for' definitions* init=expr ',' cond=expr ',' step=expr 'do' body=scoped 'od' # ForLoop
    | 'case' definitions* scrutinee=expr 'of' caseBranch ('|' caseBranch)* 'esac' # CaseExpr
    | collection=primary '[' index=expr ']' # Indexing
    | recv=primary '.' IDENT ('(' (args+=expr (',' args+=expr)* ','?)? ')')? # DotCall
    | '[' ((items+=expr) (',' items+=expr)*)? ']' # Array
    | SIDENT ('(' ((items+=expr) (',' items+=expr)*)? ')')? # Sexp
    | '{' '}' # EmptyList
    | '{' items+=expr (',' items+=expr)+ '}' # ListCtor
    ;

caseBranch
    : pattern '->' scoped
    ;

pattern
    : SIDENT ('(' ((items+=pattern) (',' items+=pattern)*)? ')')? # SexpPatt
    | IDENT # Binding
    | IDENT '@' pattern # AsPatt
    | '_' # Wildcard
    | '{' '}' # EmptyListPatt
    | '{' items+=pattern (',' items+=pattern)+ '}' # ListPatt
    | head=pattern ':' tail=pattern # ConsPatt
    | '[' (items+=pattern)? (',' items+=pattern)+ ']' # ArrayPatt
    | NUM # NumPatt
    | STRING # StringPatt
    | CHAR # CharPatt
    | 'true' # TruePatt
    | 'false' # FalsePatt
    | '#' 'box' # TBoxPatt
    | '#' 'val' # TValPatt
    | '#' 'str' # TStrPatt
    | '#' 'array' # TArrayPatt
    | '#' 'sexp' # TSexpPatt
    | '#' 'fun' # TFunPatt
    | '(' pattern ')' # ParenthesisedPattern
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

IDENT : [a-z][A-Za-z0-9_']* ;
SIDENT : [A-Z][A-Za-z0-9_']* ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> skip ;
OP : [+\-*/%<=>!&:]+ ;
STRING : '"' ( ~'"' | '""' )* '"' ;
CHAR : '\'' ( ~'\'' | '\'\'' | '\n' | '\t' ) '\'';
