package DotsBoxes.board;

import java.util.ArrayList;
import java.util.List;

/**
 * Plateau du jeu Dots and Boxes (Points et Cases).
 * <p>
 * Le plateau est défini par une grille de points de dimension
 * {@code rows x cols}. Les segments jouables sont :
 * </p>
 * <ul>
 *   <li>horizontaux : matrice {@code rows x (cols - 1)}</li>
 *   <li>verticaux   : matrice {@code (rows - 1) x cols}</li>
 * </ul>
 * <p>
 * Les cases appartiennent à la matrice {@code (rows - 1) x (cols - 1)}.
 * Une case vaut {@code -1} tant qu'elle n'est pas capturée, puis contient
 * l'identifiant du joueur propriétaire ({@code 0} ou {@code 1}).
 * </p>
 */
public class Board {

    private final int rows;
    private final int cols;

    // Représentation correcte
    // hEdges : rows × (cols-1)  (une ligne d'arêtes horizontales par ligne de points)
    // vEdges : (rows-1) × cols  (une colonne d'arêtes verticales par colonne de points)
    // boxes  : (rows-1) × (cols-1)
    private final boolean[][] hEdges;
    private final boolean[][] vEdges;
    private final int[][] boxes;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE  = "\u001B[34m";
    private static final String ANSI_RED   = "\u001B[31m";

    /* ===================== CONSTRUCTEURS ===================== */

    /**
     * Construit un nouveau plateau vide.
     *
     * @param rows nombre de lignes de points (>= 2 recommandé)
     * @param cols nombre de colonnes de points (>= 2 recommandé)
     */
    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        this.hEdges = new boolean[rows][cols - 1];
        this.vEdges = new boolean[rows - 1][cols];
        this.boxes  = new int[rows - 1][cols - 1];

        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                boxes[r][c] = -1;
            }
        }
    }

    /**
     * Construit une copie profonde d'un autre plateau.
     * <p>
     * Les tableaux internes sont dupliqués afin que la nouvelle instance
     * puisse être modifiée indépendamment de l'originale.
     * </p>
     *
     * @param other plateau source à copier
     */
    public Board(Board other) {
        this.rows = other.rows;
        this.cols = other.cols;

        this.hEdges = new boolean[rows][cols - 1];
        this.vEdges = new boolean[rows - 1][cols];
        this.boxes  = new int[rows - 1][cols - 1];

        for (int r = 0; r < rows; r++) {
            if (cols - 1 > 0) System.arraycopy(other.hEdges[r], 0, this.hEdges[r], 0, cols - 1);
            if (r < rows - 1) System.arraycopy(other.boxes[r], 0, this.boxes[r], 0, cols - 1);
        }

        for (int r = 0; r < rows - 1; r++) {
            System.arraycopy(other.vEdges[r], 0, this.vEdges[r], 0, cols);
        }
    }

    /* ===================== VALIDATION ===================== */

    /**
     * Indique si une action est jouable sur l'état courant.
     *
     * @param action action candidate
     * @return {@code true} si les coordonnées sont dans les bornes et si
     *         le segment visé n'est pas encore tracé
     */
    public boolean isValid(Action action) {
        int r = action.getRow();
        int c = action.getCol();

        return switch (action.getType()) {
            case HORIZONTAL -> r >= 0 && r < rows && c >= 0 && c < cols - 1 && !hEdges[r][c];
            case VERTICAL   -> r >= 0 && r < rows - 1 && c >= 0 && c < cols && !vEdges[r][c];
        };
    }

    /* ===================== APPLICATION D'UN COUP ===================== */

    /**
     * Applique un coup sur le plateau et retourne le nombre de cases fermées.
     * <p>
     * Si le segment joué ferme une ou deux cases, ces cases sont attribuées
     * à {@code playerId}.
     * </p>
     *
     * @param action action à appliquer
     * @param playerId identifiant du joueur propriétaire des cases capturées
     * @return nombre de cases fermées par ce coup (0, 1 ou 2)
     * @throws IllegalArgumentException si l'action est invalide
     */
    public int apply(Action action, int playerId) {
        if (!isValid(action)) throw new IllegalArgumentException("Coup invalide");

        int closed = 0;
        int r = action.getRow();
        int c = action.getCol();

        if (action.getType() == Action.Type.HORIZONTAL) {
            hEdges[r][c] = true;

            // Case au-dessus
            if (r > 0 && isBoxClosed(r - 1, c)) {
                boxes[r - 1][c] = playerId;
                closed++;
            }

            // Case en dessous
            if (r < rows - 1 && isBoxClosed(r, c)) {
                boxes[r][c] = playerId;
                closed++;
            }

        } else { // VERTICAL
            vEdges[r][c] = true;

            // Case à gauche
            if (c > 0 && isBoxClosed(r, c - 1)) {
                boxes[r][c - 1] = playerId;
                closed++;
            }

            // Case à droite
            if (c < cols - 1 && isBoxClosed(r, c)) {
                boxes[r][c] = playerId;
                closed++;
            }
        }

        return closed;
    }

    /**
     * Annule un coup joué précédemment
     * Il est supposé qu'il est possible de revenir en arrière
     * car si le coup était incorrect il aurait levé une exception
     * au moment où il est joué.
     * @param action action à rollback
     */
    public void undo(Action action) {
        int r = action.getRow();
        int c = action.getCol();

        if (action.getType() == Action.Type.HORIZONTAL) {
            hEdges[r][c] = false;

            // Case au-dessus
            if (r > 0 && boxes[r - 1][c] != -1) {
                boxes[r - 1][c] = -1;
            }

            // Case en dessous
            if (r < rows - 1 && boxes[r][c] != -1) {
                boxes[r][c] = -1;
            }

        } else { // VERTICAL
            vEdges[r][c] = false;

            // Case à gauche
            if (c > 0 && boxes[r][c-1] != -1) {
                boxes[r][c - 1] = -1;
            }

            // Case à droite
            if (c < cols - 1 && boxes[r][c] != -1) {
                boxes[r][c] = -1;
            }
        }
    }

    /**
     * Vérifie si une case est complètement fermée et encore libre.
     *
     * @param r ligne de la case
     * @param c colonne de la case
     * @return {@code true} si les quatre côtés sont tracés et que la case
     *         n'appartient encore à aucun joueur
     */
    private boolean isBoxClosed(int r, int c) {
        // Vérification des bornes
        if (r < 0 || r >= boxes.length || c < 0 || c >= boxes[0].length) return false;

        // Les 4 côtés
        boolean top    = hEdges[r][c];
        boolean bottom = hEdges[r + 1][c];
        boolean left   = vEdges[r][c];
        boolean right  = vEdges[r][c + 1];

        return boxes[r][c] == -1 && top && bottom && left && right;
    }

    /* ===================== ACTIONS DISPONIBLES ===================== */


    /**
     * Retourne la liste de tous les coups actuellement disponibles.
     *
     * @return liste des segments non encore tracés
     */
    public List<Action> getAvailableActions() {
        List<Action> actions = new ArrayList<>();

        // Arêtes horizontales : rows × (cols-1)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols - 1; c++) {
                if (!hEdges[r][c]) actions.add(new Action(Action.Type.HORIZONTAL, r, c));
            }
        }

        // Arêtes verticales : (rows-1) × cols
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols; c++) {
                if (!vEdges[r][c]) actions.add(new Action(Action.Type.VERTICAL, r, c));
            }
        }

        return actions;
    }

    /**
     * @return true si une action ferme une boite false sinon
     */
    public boolean isActionClosing(Action action) {
        return countClosing(action) > 0;
    }

    public boolean isActionGiving(Action action) {
        return countGiving(action) > 0;
    }


    public List<Action> getSortedActions() {
        List<Action> actions = getAvailableActions();

        List<Action> closing2 = new ArrayList<>(); // ferme 2 cases → priorité absolue
        List<Action> closing1 = new ArrayList<>(); // ferme 1 case
        List<Action> neutral  = new ArrayList<>(); // aucun impact immédiat
        List<Action> giving1  = new ArrayList<>(); // offre 1 case à l'adversaire
        List<Action> giving2  = new ArrayList<>(); // offre 2 cases → à éviter absolument

        for (Action action : actions) {
            int closes = countClosing(action); // 0, 1 ou 2
            int gives  = countGiving(action);  // 0, 1 ou 2

            if      (closes == 2) closing2.add(action);
            else if (closes == 1) closing1.add(action);
            else if (gives  == 2) giving2.add(action);
            else if (gives  == 1) giving1.add(action);
            else                  neutral.add(action);
        }

        List<Action> sorted = new ArrayList<>();
        sorted.addAll(closing2);
        sorted.addAll(closing1);
        sorted.addAll(neutral);
        sorted.addAll(giving1);
        sorted.addAll(giving2);
        return sorted;
    }

    public int countClosing(Action action) {
        int count = 0;
        int r = action.getRow();
        int c = action.getCol();

        if (action.getType() == Action.Type.HORIZONTAL) {
            if (r < rows - 1 && c < cols - 1
                    && hEdges[r+1][c] && vEdges[r][c] && vEdges[r][c+1]) count++;
            if (r > 0 && c < cols - 1
                    && hEdges[r-1][c] && vEdges[r-1][c] && vEdges[r-1][c+1]) count++;
        } else {
            if (r < rows - 1 && c < cols - 1
                    && hEdges[r][c] && hEdges[r+1][c] && vEdges[r][c+1]) count++;
            if (r < rows - 1 && c > 0
                    && hEdges[r][c-1] && hEdges[r+1][c-1] && vEdges[r][c-1]) count++;
        }
        return count; // 0, 1 ou 2
    }

    public int countGiving(Action action) {
        int count = 0;
        int r = action.getRow();
        int c = action.getCol();

        if (action.getType() == Action.Type.HORIZONTAL) {
            if (r < rows - 1 && c < cols - 1) {
                int sides = (hEdges[r+1][c] ? 1 : 0) + (vEdges[r][c] ? 1 : 0) + (vEdges[r][c+1] ? 1 : 0);
                if (sides == 2) count++;
            }
            if (r > 0 && c < cols - 1) {
                int sides = (hEdges[r-1][c] ? 1 : 0) + (vEdges[r-1][c] ? 1 : 0) + (vEdges[r-1][c+1] ? 1 : 0);
                if (sides == 2) count++;
            }
        } else {
            if (r < rows - 1 && c < cols - 1) {
                int sides = (hEdges[r][c] ? 1 : 0) + (hEdges[r+1][c] ? 1 : 0) + (vEdges[r][c+1] ? 1 : 0);
                if (sides == 2) count++;
            }
            if (r < rows - 1 && c > 0) {
                int sides = (hEdges[r][c-1] ? 1 : 0) + (hEdges[r+1][c-1] ? 1 : 0) + (vEdges[r][c-1] ? 1 : 0);
                if (sides == 2) count++;
            }
        }
        return count; // 0, 1 ou 2
    }



    /* ===================== SCORES ===================== */

    /**
     * Calcule le score courant d'un joueur sur le plateau.
     *
     * @param playerId identifiant du joueur
     * @return nombre de cases capturées par ce joueur
     */
    public int getScore(int playerId) {
        int score = 0;
        for (int r = 0; r < rows - 1; r++)
            for (int c = 0; c < cols - 1; c++)
                if (boxes[r][c] == playerId) score++;
        return score;
    }

    /**
     * Indique si la partie est terminée (aucun segment libre).
     *
     * @return {@code true} si aucun coup n'est encore possible
     */
    public boolean isFinished() {
        return getAvailableActions().isEmpty();
    }

    /* ===================== GETTERS ===================== */

    /**
     * Retourne le nombre de lignes de points du plateau.
     *
     * @return nombre de lignes
     */
    public int getRows() { return rows; }

    /**
     * Retourne le nombre de colonnes de points du plateau.
     *
     * @return nombre de colonnes
     */
    public int getCols() { return cols; }

    /**
     * Retourne la matrice des segments horizontaux.
     * <p>
     * {@code true} signifie que le segment est déjà tracé.
     * </p>
     *
     * @return matrice des arêtes horizontales
     */
    public boolean[][] getHEdges() { return hEdges; }

    /**
     * Retourne la matrice des segments verticaux.
     * <p>
     * {@code true} signifie que le segment est déjà tracé.
     * </p>
     *
     * @return matrice des arêtes verticales
     */
    public boolean[][] getVEdges() { return vEdges; }

    /**
     * Retourne le propriétaire d'une case.
     *
     * @param r ligne de la case
     * @param c colonne de la case
     * @return {@code -1} si la case est libre, sinon l'identifiant du joueur
     */
    public int getBoxOwner(int r, int c) { return boxes[r][c]; }

    /**
     * Indique si un segment horizontal est déjà tracé.
     *
     * @param r ligne du segment
     * @param c colonne du segment
     * @return {@code true} si le segment est tracé
     */
    public boolean isHEdgeSet(int r, int c) { return hEdges[r][c]; }

    /**
     * Indique si un segment vertical est déjà tracé.
     *
     * @param r ligne du segment
     * @param c colonne du segment
     * @return {@code true} si le segment est tracé
     */
    public boolean isVEdgeSet(int r, int c) { return vEdges[r][c]; }

    /* ===================== AFFICHAGE ASCII ===================== */

    /**
     * Affiche une représentation ASCII colorisée du plateau.
     * <p>
     * Les cases capturées sont affichées avec un symbole :
     * bleu pour le joueur {@code 0}, rouge pour le joueur {@code 1}.
     * </p>
     */
    public void printAscii() {
        for (int r = 0; r < rows; r++) {
            // ligne des points + segments horizontaux (il y a rows lignes de points)
            for (int c = 0; c < cols; c++) {
                System.out.print(".");
                if (c < cols - 1) System.out.print(hEdges[r][c] ? "---" : "   ");
            }
            System.out.println();

            // pour toutes les lignes sauf la dernière, afficher colonnes verticales + cases
            if (r < rows - 1) {
                for (int c = 0; c < cols; c++) {
                    System.out.print(vEdges[r][c] ? "|" : " ");

                    if (c < cols - 1) {
                        if (boxes[r][c] == -1) System.out.print("   ");
                        else if (boxes[r][c] == 0) System.out.print(" " + ANSI_BLUE + "■" + ANSI_RESET + " ");
                        else System.out.print(" " + ANSI_RED + "■" + ANSI_RESET + " ");
                    }
                }
                System.out.println();
            }
        }
    }
}
