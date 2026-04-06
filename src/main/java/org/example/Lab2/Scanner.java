package org.example.Lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Scanner {

    private static final Set<String> KEYWORDS = Set.of(
            "int", "double", "float", "boolean", "char", "byte", "short", "long",
            "var", "void", "return", "String", "new", "const"
    );

    public static List<Token> analyze(String text) {
        List<Token> tokens = new ArrayList<>();
        int len = text.length();
        int pos = 0;
        int line = 1;
        int col = 1;

        while (pos < len) {
            char c = text.charAt(pos);

            if (Character.isWhitespace(c)) {
                int startPos = col;
                int startGlobal = pos;
                StringBuilder ws = new StringBuilder();
                while (pos < len && Character.isWhitespace(text.charAt(pos))) {
                    char wc = text.charAt(pos);
                    ws.append(wc);
                    pos++;
                    if (wc == '\n') {
                        line++;
                        col = 1;
                    } else {
                        col++;
                    }
                }
                tokens.add(new Token(11, "разделитель (пробел/перенос)", "(пробел/перенос)", line, startPos, col - 1, startGlobal, pos - 1));
                continue;
            }

            if (Character.isLetter(c) || c == '_' || c == '$') {
                int startPos = col;
                int startGlobal = pos;
                StringBuilder id = new StringBuilder();
                while (pos < len && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_' || text.charAt(pos) == '$')) {
                    id.append(text.charAt(pos));
                    pos++;
                    col++;
                }
                String lexeme = id.toString();
                if (KEYWORDS.contains(lexeme)) {
                    tokens.add(new Token(14, "ключевое слово", lexeme, line, startPos, col - 1, startGlobal, pos - 1));
                } else {
                    tokens.add(new Token(2, "идентификатор", lexeme, line, startPos, col - 1, startGlobal, pos - 1));
                }
                continue;
            }

            if (Character.isDigit(c)) {
                int startPos = col;
                int startGlobal = pos;
                StringBuilder num = new StringBuilder();
                while (pos < len && Character.isDigit(text.charAt(pos))) {
                    num.append(text.charAt(pos));
                    pos++;
                    col++;
                }

                if (pos < len && text.charAt(pos) == '.') {
                    num.append('.');
                    pos++;
                    col++;
                    while (pos < len && Character.isDigit(text.charAt(pos))) {
                        num.append(text.charAt(pos));
                        pos++;
                        col++;
                    }
                    tokens.add(new Token(6, "вещественное число", num.toString(), line, startPos, col - 1, startGlobal, pos - 1));
                } else {
                    tokens.add(new Token(1, "целое число", num.toString(), line, startPos, col - 1, startGlobal, pos - 1));
                }
                continue;
            }

            if ("-+*/=".indexOf(c) != -1) {
                int startPos = col;
                int startGlobal = pos;
                StringBuilder op = new StringBuilder();
                op.append(c);
                pos++;
                col++;

                if (pos < len) {
                    char nextC = text.charAt(pos);
                    if ((c == '-' && nextC == '>') || 
                            (c == '+' && nextC == '+') || 
                            (c == '-' && nextC == '-')) { 
                        op.append(nextC);
                        pos++;
                        col++;
                    }
                }

                String lexeme = op.toString();
                if (lexeme.equals("->")) {
                    tokens.add(new Token(3, "лямбда оператор", lexeme, line, startPos, col - 1, startGlobal, pos - 1));
                } else {
                    tokens.add(new Token(10, "оператор", lexeme, line, startPos, col - 1, startGlobal, pos - 1));
                }
                continue;
            }

            if ("(){},;.".indexOf(c) != -1) {
                int startPos = col;
                int startGlobal = pos;
                pos++;
                col++;
                int code = (c == ';') ? 16 : 4;
                String typeName = (c == ';') ? "конец оператора" : "разделитель";
                tokens.add(new Token(code, typeName, String.valueOf(c), line, startPos, col - 1, startGlobal, pos - 1));
                continue;
            }

            if (c == '"' || c == '\'') {
                int startPos = col;
                int startGlobal = pos;
                char quote = c;
                StringBuilder str = new StringBuilder();
                str.append(c);
                pos++;
                col++;
                boolean closed = false;
                while (pos < len) {
                    char nc = text.charAt(pos);
                    str.append(nc);
                    pos++;
                    col++;
                    if (nc == quote) {
                        closed = true;
                        break;
                    }
                    if (nc == '\n') {
                        line++;
                        col = 1;
                    }
                }

                if (closed) {
                    tokens.add(new Token(5, "строковый литерал", str.toString(), line, startPos, col - 1, startGlobal, pos - 1));
                } else {
                    tokens.add(new Token(17, "ошибка (незакрытая строка)", str.toString(), line, startPos, col - 1, startGlobal, pos - 1));
                }
                continue;
            }

            int startPos = col;
            int startGlobal = pos;
            StringBuilder err = new StringBuilder();
            while (pos < len) {
                char check = text.charAt(pos);
                if (Character.isWhitespace(check) || Character.isLetterOrDigit(check) || check == '_' || check == '$' || "-+*/=(){},;.\"\'".indexOf(check) != -1) {
                    break;
                }
                err.append(check);
                pos++;
                col++;
            }
            tokens.add(new Token(17, "ошибка (недопустимые символы)", err.toString(), line, startPos, col - 1, startGlobal, pos - 1));
        }

        return tokens;
    }
}
