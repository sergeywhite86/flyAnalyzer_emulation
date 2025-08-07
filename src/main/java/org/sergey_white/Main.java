package org.sergey_white;


import org.sergey_white.service.FlyAnalyzer;

public class Main {
    public static void main(String[] args) {
        FlyAnalyzer analyzer = new FlyAnalyzer();
        analyzer.analyze("tickets.json");
    }
}
