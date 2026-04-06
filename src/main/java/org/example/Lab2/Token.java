package org.example.Lab2;

public class Token {
    private final int code;
    private final String type;
    private final String text;
    private final int line;
    private final int startCol;
    private final int endCol;
    private final int globalStart;
    private final int globalEnd;

    public Token(int code, String type, String text, int line, int startCol, int endCol, int globalStart, int globalEnd) {
        this.code = code;
        this.type = type;
        this.text = text;
        this.line = line;
        this.startCol = startCol;
        this.endCol = endCol;
        this.globalStart = globalStart;
        this.globalEnd = globalEnd;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }
    
    public String getLocation() {
        return "строка " + line + ", позиция " + startCol;
    }

    public int getLine() {
        return line;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getEndCol() {
        return endCol;
    }

    public int getGlobalStart() {
        return globalStart;
    }

    public int getGlobalEnd() {
        return globalEnd;
    }
}
