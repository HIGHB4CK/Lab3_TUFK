package org.example.Lab2;

public class SyntaxError {
    private final String fragment;
    private final String location;
    private final String description;
    private final int globalStart;
    private final int globalEnd;

    public SyntaxError(String fragment, String location, String description, int globalStart, int globalEnd) {
        this.fragment = fragment;
        this.location = location;
        this.description = description;
        this.globalStart = globalStart;
        this.globalEnd = globalEnd;
    }

    public String getFragment() {
        return fragment;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public int getGlobalStart() {
        return globalStart;
    }

    public int getGlobalEnd() {
        return globalEnd;
    }
}
