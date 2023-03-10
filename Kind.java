package core;

public enum Kind {
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    COMMA,
    SEMICOLON,
    DOT,
    COLON,

    // keywords
    AS,
    LOCAL,
    PUBLIC,
    CONST,
    IF,
    THEN,
    ELSE,
    ENDIF,
    ELSEIF,
    TRUE,
    FALSE,
    NULL,
    RETURN,
    DO,
    CASE,
    OTHERWISE,
    ENDCASE,
    DODEFAULT,
    THIS,
    CREATEOBJECT,
    FUNCTION,
    LPARAMETERS,
    ENDFUNC,
    PRINT,
    RELEASE,
    DEFER,
    ENDDEFER,

    // Iterators keywords
    WHILE,
    ENDWHILE,
    ENDDO,
    REPEAT,
    UNTIL,
    CLASS,
    ENDCLASS,
    FOR,
    TO,
    STEP,
    PRIVATE,
    IMPORT,
    MODULE,
    ENDMODULE,
    ENDFOR, 
    EXIT,
    LOOP,

    // Literals
    NUMBER,
    STRING,
    IDENTIFIER,

    // Operators
    SIMPLE_ASSIGN,    
    COMPLEX_ASSIGN,
    RELATIONAL_OPERATOR,
    EQUALITY_OPERATOR,
    TERM_OPERATOR,
    FACTOR_OPERATOR,
    LOGICAL_OR,
    LOGICAL_AND,
    LOGICAL_NOT,
    IGNORE,
    EOF,
    ERROR,
    QUESTION,
}
