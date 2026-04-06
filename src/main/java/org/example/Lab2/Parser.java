package org.example.Lab2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Parser {

    private final List<Token> tokens;
    private int pos;
    private final List<SyntaxError> errors;

    public Parser(List<Token> allTokens) {
        this.tokens = new ArrayList<>();
        this.errors = new ArrayList<>();
        for (Token t : allTokens) {
            if (t.getType().startsWith("ошибка")) {
                this.errors.add(new SyntaxError(t.getText(), t.getLocation(), "Лексическая ошибка: " + t.getType(), t.getGlobalStart(), t.getGlobalEnd()));
            } else if (t.getCode() != 11) {
                this.tokens.add(t);
            }
        }
        this.pos = 0;
    }

    public static List<SyntaxError> parse(List<Token> tokens) {
        Parser parser = new Parser(tokens);
        parser.parseZ();
        
        List<SyntaxError> mergedErrors = new ArrayList<>();
        if (!parser.errors.isEmpty()) {
            SyntaxError prev = parser.errors.get(0);
            for (int i = 1; i < parser.errors.size(); i++) {
                SyntaxError curr = parser.errors.get(i);
                
                boolean hasTokensBetween = false;
                for (Token t : parser.tokens) {
                    if (t.getGlobalStart() > prev.getGlobalEnd() && t.getGlobalEnd() < curr.getGlobalStart()) {
                        hasTokensBetween = true;
                        break;
                    }
                }
                
                if (!hasTokensBetween && prev.getDescription().equals(curr.getDescription())) {
                    String newFragment;
                    if (prev.getGlobalEnd() >= curr.getGlobalStart() - 1) {
                        newFragment = prev.getFragment() + curr.getFragment();
                    } else {
                        newFragment = prev.getFragment() + " " + curr.getFragment();
                    }
                    prev = new SyntaxError(newFragment, prev.getLocation(), prev.getDescription(), prev.getGlobalStart(), curr.getGlobalEnd());
                } else {
                    mergedErrors.add(prev);
                    prev = curr;
                }
            }
            mergedErrors.add(prev);
        }
        return mergedErrors;
    }

    private void addError(String description) {
        if (pos < tokens.size()) {
            Token t = tokens.get(pos);
            errors.add(new SyntaxError(t.getText(), t.getLocation(), description, t.getGlobalStart(), t.getGlobalEnd()));
        } else if (!tokens.isEmpty()) {
            Token t = tokens.get(tokens.size() - 1);
            errors.add(new SyntaxError("EOF", "конец файла", description, t.getGlobalEnd(), t.getGlobalEnd()));
        } else {
            errors.add(new SyntaxError("-", "-", description, 0, 0));
        }
    }

    private void recover(String... follow) {
        Set<String> followSet = new HashSet<>(Arrays.asList(follow));
        followSet.add(";");
        followSet.add("}");
        
        int tempPos = pos;
        boolean foundSync = false;
        while (tempPos < tokens.size()) {
            if (followSet.contains(tokens.get(tempPos).getText())) {
                foundSync = true;
                break;
            }
            tempPos++;
        }

        if (foundSync) {
            while (!isEOF()) {
                Token c = current();
                if (followSet.contains(c.getText())) {
                    break;
                }
                advance();
            }
        } else {
            if (!isEOF()) advance();
        }
    }

    private boolean match(String expectedText, String... follow) {
        Token c = current();
        if (c != null && c.getText().equals(expectedText)) {
            advance();
            return true;
        }
        addError("Ожидалось '" + expectedText + "'");
        recover(follow);
        return false;
    }

    private boolean matchType(int expectedCode, String expectedTypeDesc, String... follow) {
        Token c = current();
        if (c != null && c.getCode() == expectedCode) {
            advance();
            return true;
        }
        addError("Ожидалось " + expectedTypeDesc);
        recover(follow);
        return false;
    }

    private Token current() {
        if (pos < tokens.size()) return tokens.get(pos);
        return null;
    }

    private void advance() {
        if (pos < tokens.size()) pos++;
    }

    private boolean isEOF() {
        return pos >= tokens.size();
    }

    private void parseZ() {
        if (isEOF()) return;

        Token c = current();
        if (c != null && c.getText().equals(";")) {
             addError("Ожидалось лямбда-выражение (левая часть, =, и само выражение)");
             advance();
             return;
        }

        boolean hasAssignment = false;
        int tempPos = pos;
        while (tempPos < tokens.size()) {
             if (tokens.get(tempPos).getText().equals("->")) {
                 break;
             }
             if (tokens.get(tempPos).getText().equals(";") || tokens.get(tempPos).getText().equals("{")) {
                 break;
             }
             if (tokens.get(tempPos).getText().equals("=")) {
                 hasAssignment = true;
                 break;
             }
             tempPos++;
        }

        if (hasAssignment) {
            c = current();
            if (c != null && (c.getCode() == 14 || c.getText().equals("const"))) { 
                advance();
                matchType(2, "идентификатор", "=");
            } else if (c != null && c.getCode() == 2) {
                advance();
                if (current() != null && current().getCode() == 2) {
                    advance();
                }
            } else {
                addError("Ожидалась левая часть присваивания");
                recover("=");
            }
            match("=", "->", "(");
        } else {
            addError("Ожидалось присваивание переменной (отсутствует знак '=')");
        }
        
        c = current();
        if (c == null || c.getText().equals(";")) {
            if (hasAssignment) {
                addError("Ожидалось лямбда-выражение");
            }
        } else {
            parseLambda();
        }
        
        if (!isEOF() && !current().getText().equals(";")) {
             while(!isEOF() && !current().getText().equals(";")) {
                 addError("Ожидался конец выражения, найдены лишние символы");
                 advance();
             }
        }
        
        match(";", "EOF");
        
        if (!isEOF()) {
             while(!isEOF()) {
                 addError("Ожидался конец выражения, найдены лишние символы");
                 advance();
             }
        }
    }

    private void parseLambda() {
        parseParams("->");
        match("->", "{", "return");
        parseBody("");
    }

    private void parseParams(String... follow) {
        Token c = current();
        if (c == null) {
            addError("Ожидались параметры лямбда-выражения");
            return;
        }
        
        if (c.getText().equals("(")) {
            advance();
            c = current();
            if (c != null && !c.getText().equals(")")) {
                parseParamList(")");
            }
            match(")", "->");
        } else if (c.getCode() == 2) { 
            advance();
        } else {
            addError("Ожидались параметры лямбда-выражения");
            recover(follow);
        }
    }

    private void parseParamList(String... follow) {
        parseParam(",", ")");
        parseParamListTail(")");
    }

    private void parseParamListTail(String... follow) {
        while (!isEOF()) {
            Token c = current();
            if (c != null && c.getText().equals(",")) {
                advance();
                parseParam(",", ")");
            } else {
                break;
            }
        }
    }

    private void parseParam(String... follow) {
        Token c = current();
        if (c == null) return;

        if (c.getCode() == 14) {
            advance();
            matchType(2, "идентификатор параметра", follow);
        } else if (c.getCode() == 2) {
            advance();
        } else {
            addError("Ожидался параметр (идентификатор или тип с идентификатором)");
            recover(follow);
        }
    }

    private void parseBody(String... follow) {
        Token c = current();
        if (c == null) {
            addError("Ожидалось тело лямбда-выражения");
            return;
        }

        if (c.getText().equals("{")) {
            advance();
            parseStmtList("}");
            match("}", follow);
        } else if (c.getText().equals(";")) {
            addError("Ожидалось тело лямбда-выражения");
        } else {
            parseExpr(";", "EOF");
        }
    }

    private void parseStmtList(String... follow) {
        while (!isEOF()) {
            Token c = current();
            if (c != null && c.getText().equals("}")) {
                break;
            }
            parseStmt("}", ";", "return");
        }
    }

    private void parseStmt(String... follow) {
        Token c = current();
        if (c == null) return;

        if (c.getText().equals("return")) {
            advance();
            parseExpr(";");
            match(";", "}", "return");
        } else if (c.getCode() == 14) { 
            advance(); 
            matchType(2, "идентификатор переменной", "=", ";");
            if (current() != null && current().getText().equals("=")) {
                advance();
                parseExpr(";");
            }
            match(";", "}", "return");
        } else {
            parseExpr(";");
            match(";", "}", "return");
        }
    }

    private void parseExpr(String... follow) {
        List<String> termFollows = new ArrayList<>(Arrays.asList("+", "-", "*", "/", "="));
        termFollows.addAll(Arrays.asList(follow));
        parseTerm(termFollows.toArray(new String[0]));
        parseExprTail(follow);
    }

    private void parseExprTail(String... follow) {
        while (!isEOF()) {
            Token c = current();
            if (c != null && isOp(c.getText())) {
                advance();
                List<String> termFollows = new ArrayList<>(Arrays.asList("+", "-", "*", "/", "="));
                termFollows.addAll(Arrays.asList(follow));
                parseTerm(termFollows.toArray(new String[0]));
            } else {
                break;
            }
        }
    }

    private boolean isOp(String text) {
        return Arrays.asList("+", "-", "*", "/", "=").contains(text);
    }

    private void parseTerm(String... follow) {
        Token c = current();
        if (c == null) {
            addError("Ожидался операнд (идентификатор, число, строка или выражение в скобках)");
            return;
        }

        if (c.getCode() == 2) {
            advance();
            c = current();
            if (c != null && c.getText().equals(".")) {
                while (c != null && c.getText().equals(".")) {
                    advance();
                    matchType(2, "идентификатор атрибута/метода", "(", ";", ")");
                    c = current();
                }
                c = current();
                if (c != null && c.getText().equals("(")) {
                    advance();
                    parseArgs(")");
                    match(")", follow);
                }
            } else if (c != null && c.getText().equals("(")) {
                advance();
                parseArgs(")");
                match(")", follow);
            }
        } else if (c.getCode() == 1 || c.getCode() == 6) { 
            advance();
        } else if (c.getCode() == 5) {
            advance();
        } else if (c.getText().equals("(")) {
            advance();
            parseExpr(")");
            match(")", follow);
        } else if (c.getText().equals(".")) {
            Token next = pos + 1 < tokens.size() ? tokens.get(pos + 1) : null;
            if (next != null && next.getCode() == 1) {
                addError("Ожидалась цифра перед десятичной точкой");
            } else {
                addError("Ожидался операнд (идентификатор, число, строка или выражение в скобках)");
            }
            advance();
        } else {
            addError("Ожидался операнд (идентификатор, число, строка или выражение в скобках)");
            recover(follow);
        }
    }

    private void parseArgs(String... follow) {
        Token c = current();
        if (c == null || c.getText().equals(")")) return;
        
        parseExpr(",", ")");
        parseArgsTail(")");
    }

    private void parseArgsTail(String... follow) {
        while (!isEOF()) {
            Token c = current();
            if (c != null && c.getText().equals(",")) {
                advance();
                parseExpr(",", ")");
            } else {
                break;
            }
        }
    }
}
