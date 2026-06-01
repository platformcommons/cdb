package com.platformcommons.security.model;

import java.awt.Color;

public enum SeverityLevel {
    CRITICAL("Critical", new Color(169, 17, 1), 5),
    HIGH("High",         new Color(204, 85, 0),  4),
    MEDIUM("Medium",     new Color(184, 134, 11), 3),
    LOW("Low",           new Color(0, 102, 153),  2),
    INFO("Info",         new Color(70, 130, 180), 1),
    PASS("Pass",         new Color(34, 139, 34),  0);

    private final String label;
    private final Color color;
    private final int weight;

    SeverityLevel(String label, Color color, int weight) {
        this.label  = label;
        this.color  = color;
        this.weight = weight;
    }

    public String getLabel()  { return label; }
    public Color  getColor()  { return color; }
    public int    getWeight() { return weight; }
}
