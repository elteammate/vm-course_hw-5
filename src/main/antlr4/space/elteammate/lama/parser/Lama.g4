grammar Lama;

module : definitions* expr EOF;

definitions
    : 'var' varDef (',' varDef)* ';' # VarDefinitions
    | 'fun' IDENT funParams funBody # FunDefinition
    ;

varDef
    : IDENT ('=' simpleExpr)?
    ;

funParams
    : '(' ')' # EmptyParams
    | '(' funParam (',' funParam)* ')' # Params
    ;

funParam
    : IDENT # ParamIdent
    | pattern # ParamPattern
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
    | 'true' # True
    | 'false' # False
    | IDENT '(' (args+=expr (',' args+=expr)* ','?)? ')' # DirectCall
    | fn=primary '(' (args+=expr (',' args+=expr)* ','?)? ')' # IndirectCall
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
    | 'fun' funParams funBody # InlineFn
    | 'let' pattern '=' scrutinee=expr 'in' rest=expr # LetExpr
    | 'eta' f=primary # Eta
    ;

caseBranch
    : pattern '->' scoped
    ;

pattern
    : simplePattern # SimplePatt
    | head=simplePattern ':' tail=pattern # ConsPatt
    ;

simplePattern
    : SIDENT ('(' ((items+=pattern) (',' items+=pattern)*)? ')')? # SexpPatt
    | IDENT # Binding
    | IDENT '@' pattern # AsPatt
    | '_' # Wildcard
    | '{' '}' # EmptyListPatt
    | '{' items+=pattern (',' items+=pattern)* '}' # ListPatt
    | '[' ((items+=pattern) (',' items+=pattern)*)? ']' # ArrayPatt
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
    : primary (op primary)* # RawExpr
    ;

op
    : OP
    | ':'
    ;

lvalue
    : IDENT # LLookup
    | collection=primary '[' index=expr ']' # LIndexing
    ;

expr
    : simpleExpr # Simple
    | lvalue ':=' expr # Assignment
    | expr ';' expr # Seq
    ;

IDENT : [a-z][A-Za-z0-9_']* ;
SIDENT : [A-Z][A-Za-z0-9_']* ;
NUM : [0-9]+ ;
COMMENT : '--' ~[\r\n]* -> skip ;
WS : [ \t\r\n]+ -> skip ;
MULTILINE_COMMENT : '(*' .*? '*)' -> skip ;
OP : [+\-*/%<=>!&:]+ ;
STRING : '"' ( ~'"' | '""' )* '"' ;
CHAR : '\'' ( ~'\'' | '\'\'' | '\n' | '\t' ) '\'';
