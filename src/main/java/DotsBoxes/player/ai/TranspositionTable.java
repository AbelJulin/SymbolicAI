package DotsBoxes.player.ai;

import DotsBoxes.board.Action;

import java.util.HashMap;
import java.util.Map;

/**
 * Table de transposition pour éviter de recalculer des positions déjà vues.
 * C'est un cache qui stocke les résultats de l'Alpha-Beta.
 */
public class TranspositionTable {

    // Nombre maximum d'entrées stockées simultanément en mémoire
    private static final int TAILLE_MAX = 1_000_000;

    // La table qui fait le lien entre un Hash Zobrist et une évaluation
    private final Map<Long, TTEntry> table;

    // Pour voir si la table est efficace (stats)
    private int hits;
    private int consultations;

    /**
     * Constructeur pour initialiser une table de transposition vide.
     */
    public TranspositionTable() {
        this.table = new HashMap<>();
        this.hits = 0;
        this.consultations = 0;
    }

    // Enregistre un résultat dans la table
    public void store(long hash, int value, int depth, TTEntry.Flag flag, Action bestAction) {
        // Vérification de la taille maximale
        if (table.size() >= TAILLE_MAX) {
            table.clear();
        }

        TTEntry entreeExistante = table.get(hash);

        // On ne remplace une entrée existante que si la nouvelle est plus profonde
        if (entreeExistante == null || depth >= entreeExistante.depth) {
            table.put(hash, new TTEntry(value, depth, flag, bestAction));
        }
    }

    // Cherche si on a déjà calculé cette position
    public TTEntry lookup(long hash) {
        consultations++;
        TTEntry entree = table.get(hash);
        if (entree != null)
            hits++;
        return entree;
    }

    // Vide la table (obligatoire entre deux parties!!)
    public void clear() {
        table.clear();
        hits = 0;
        consultations = 0;
    }

    // Pour afficher si la table a bien servi pendant le tour
    public void printStats() {
        double tauxHits = (consultations > 0)
                ? (hits * 100.0 / consultations)
                : 0.0;
        System.out.printf("Table de transposition : %d entrées | %d/%d hits (%.1f%%)%n",
                table.size(), hits, consultations, tauxHits);
    }

    // Getters pour les stats
    public int getSize() {
        return table.size();
    }

    public int getHits() {
        return hits;
    }

    public int getConsultations() {
        return consultations;
    }
}
