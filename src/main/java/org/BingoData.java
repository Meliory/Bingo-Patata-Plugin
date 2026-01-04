package org;

import org.bukkit.Material;

import java.util.*;

public class BingoData {
    private static final Map<String, Set<Material>> teamsItems = new HashMap<>();

    /**
     * Representa una línea completada en el bingo
     */
    public static class CompletedLine {
        public enum LineType {
            HORIZONTAL, VERTICAL, DIAGONAL
        }

        private final LineType type;
        private final int index; // Para horizontales/verticales: 0-4, para diagonales: 0-1

        public CompletedLine(LineType type, int index) {
            this.type = type;
            this.index = index;
        }

        public LineType getType() {
            return type;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompletedLine that = (CompletedLine) o;
            return index == that.index && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, index);
        }
    }

    public static boolean hasTeamItem(Team team, Material item){
        String teamName = team.getName();
        return teamsItems.getOrDefault(teamName, new HashSet<>()).contains(item);
    }

    public static void addTeamItem(Team team, Material item){
        String teamName = team.getName();
        teamsItems.computeIfAbsent(teamName, k -> new HashSet<>()).add(item);
    }

    public static void removeTeamItem(Team team, Material item){
        String teamName = team.getName();
        teamsItems.computeIfAbsent(teamName, k -> new HashSet<>()).remove(item);
    }

    public static Set<Material> getTeamItems(Team team){
        return teamsItems.getOrDefault(team.getName(), new HashSet<>());
    }

    public static int getTeamPoints(Team team){
        int points = 0;

        Set<Material> teamItems = teamsItems.getOrDefault(team.getName(), new HashSet<>());
        List<Material> bingoItems = BingoCard.getBingoItems();

        if(teamItems != null){
            //Suma de puntos x item (+1 por item)
            points += teamItems.size();

            //Suma de filas (+2 por fila --> vertical/horizontal/diagonal)
            points += getCompletedLinesCount(teamItems, bingoItems) * 2;

            //Suma de bingo (+5 por bingo)
            if(teamItems.size() == bingoItems.size()) {
                points += 5;
            }
        }

        return points;
    }

    public static int getCompletedLinesCount(Set<Material> teamItems, List<Material> bingoItems) {
        int completedLines = 0;

        // LÍNEAS HORIZONTALES (5 filas)
        for(int fila = 0; fila < 5; fila++) {
            if(isHorizontalLineComplete(teamItems, bingoItems, fila)) {
                completedLines++;
            }
        }

        // LÍNEAS VERTICALES (5 columnas)
        for(int columna = 0; columna < 5; columna++) {
            if(isVerticalLineComplete(teamItems, bingoItems, columna)) {
                completedLines++;
            }
        }

        // LÍNEAS DIAGONALES (2 diagonales)
        if(isDiagonal1Complete(teamItems, bingoItems)) {
            completedLines++;
        }

        if(isDiagonal2Complete(teamItems, bingoItems)) {
            completedLines++;
        }

        return completedLines;
    }

    private static boolean isHorizontalLineComplete(Set<Material> teamItems, List<Material> bingoItems, int fila) {
        for(int columna = 0; columna < 5; columna++) {
            int index = fila * 5 + columna; // Convertir (fila,columna) a index
            Material item = bingoItems.get(index);

            if(!teamItems.contains(item)) {
                return false; // Falta este item en la fila
            }
        }
        return true; // Todos los items de la fila están completos
    }

    private static boolean isVerticalLineComplete(Set<Material> teamItems, List<Material> bingoItems, int columna) {
        for(int fila = 0; fila < 5; fila++) {
            int index = fila * 5 + columna; // Convertir (fila,columna) a index
            Material item = bingoItems.get(index);

            if(!teamItems.contains(item)) {
                return false; // Falta este item en la columna
            }
        }
        return true; // Todos los items de la columna están completos
    }

    private static boolean isDiagonal1Complete(Set<Material> teamItems, List<Material> bingoItems) {
        int[] diagonalIndices = {0, 6, 12, 18, 24};

        for(int index : diagonalIndices) {
            Material item = bingoItems.get(index);
            if(!teamItems.contains(item)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDiagonal2Complete(Set<Material> teamItems, List<Material> bingoItems) {
        int[] diagonalIndices = {4, 8, 12, 16, 20};

        for(int index : diagonalIndices) {
            Material item = bingoItems.get(index);
            if(!teamItems.contains(item)) {
                return false;
            }
        }
        return true;
    }

    public static void resetTeamItems(Team team){
        if(team == null) return;

        String teamName = team.getName();
        Set<Material> items = teamsItems.get(teamName);

        if(items != null){
            items.clear();
        } else {
            teamsItems.put(teamName, new HashSet<>());
        }

        BingoScoreboard.updateTeamScoreboard(team);
    }

    /**
     * Obtiene todas las líneas completadas por un equipo
     */
    public static Set<CompletedLine> getCompletedLines(Set<Material> teamItems, List<Material> bingoItems) {
        Set<CompletedLine> completedLines = new HashSet<>();

        // LÍNEAS HORIZONTALES (5 filas)
        for(int fila = 0; fila < 5; fila++) {
            if(isHorizontalLineComplete(teamItems, bingoItems, fila)) {
                completedLines.add(new CompletedLine(CompletedLine.LineType.HORIZONTAL, fila));
            }
        }

        // LÍNEAS VERTICALES (5 columnas)
        for(int columna = 0; columna < 5; columna++) {
            if(isVerticalLineComplete(teamItems, bingoItems, columna)) {
                completedLines.add(new CompletedLine(CompletedLine.LineType.VERTICAL, columna));
            }
        }

        // LÍNEAS DIAGONALES (2 diagonales)
        if(isDiagonal1Complete(teamItems, bingoItems)) {
            completedLines.add(new CompletedLine(CompletedLine.LineType.DIAGONAL, 0));
        }

        if(isDiagonal2Complete(teamItems, bingoItems)) {
            completedLines.add(new CompletedLine(CompletedLine.LineType.DIAGONAL, 1));
        }

        return completedLines;
    }
}
