grammar Lama;

module : expr EOF;

expr
    : expr '+' NUM   # Add
    | NUM            # Number
    ;

NUM : [0-9]+ ;
WS  : [ \t\r\n]+ -> skip ;
