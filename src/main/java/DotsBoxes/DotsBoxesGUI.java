package DotsBoxes;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.ActionStrategy;
import DotsBoxes.player.ai.*;
import DotsBoxes.player.automate.GloutonActionStrategy;
import DotsBoxes.player.automate.RandomActionStrategy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DotsBoxesGUI extends JFrame {

    private Board board;
    private ActionStrategy strategieJoueur1;
    private ActionStrategy strategieJoueur2;
    private int joueurCourantId = 0;

    private final NodeCounterObserver compteurNoeuds = new NodeCounterObserver();
    private final AlphaBetaPruningObserver observateurAB = new AlphaBetaPruningObserver();

    private int tailleCellule = 60;
    private final int MARGE = 50;
    private JPanel panneauJeu;
    private JLabel etiquetteStatut;

    private JLabel lblNoeuds    = new JLabel("Nœuds : 0");
    private JLabel lblCoupures  = new JLabel("Coupures : 0");
    private JLabel lblMcts      = new JLabel("Simulations : -");
    private JLabel lblDernierTemps = new JLabel("Temps : - ms");

    public DotsBoxesGUI() {
        setTitle("Dots and Boxes - Labo IA v2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        afficherMenuSelection();
    }

    private void afficherMenuSelection() {
        JPanel conteneurPrincipal = new JPanel(new BorderLayout(20, 20));
        conteneurPrincipal.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        conteneurPrincipal.setBackground(new Color(236, 240, 241));

        JLabel titre = new JLabel("CONFIGURATION DE LA PARTIE", SwingConstants.CENTER);
        titre.setFont(new Font("Segoe UI", Font.BOLD, 24));
        conteneurPrincipal.add(titre, BorderLayout.NORTH);

        JPanel panneauJoueurs = new JPanel(new GridLayout(1, 2, 25, 0));
        panneauJoueurs.setOpaque(false);
        PanneauConfigJoueur configJ0 = new PanneauConfigJoueur(0, new Color(41, 128, 185));
        PanneauConfigJoueur configJ1 = new PanneauConfigJoueur(1, new Color(192, 57, 43));
        panneauJoueurs.add(configJ0);
        panneauJoueurs.add(configJ1);
        conteneurPrincipal.add(panneauJoueurs, BorderLayout.CENTER);

        JPanel panneauSud = new JPanel(new BorderLayout(10, 15));
        panneauSud.setOpaque(false);

        JPanel panneauTaille = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panneauTaille.setBackground(Color.WHITE);
        panneauTaille.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JSpinner spinLignes  = new JSpinner(new SpinnerNumberModel(5, 2, 20, 1));
        JSpinner spinColonnes = new JSpinner(new SpinnerNumberModel(5, 2, 20, 1));
        panneauTaille.add(new JLabel("Lignes :"));
        panneauTaille.add(spinLignes);
        panneauTaille.add(new JLabel("Colonnes :"));
        panneauTaille.add(spinColonnes);

        panneauSud.add(panneauTaille, BorderLayout.NORTH);

        JButton boutonDemarrer = new JButton("LANCER LE MATCH");
        boutonDemarrer.setFont(new Font("Segoe UI", Font.BOLD, 18));
        boutonDemarrer.setBackground(new Color(46, 204, 113));
        boutonDemarrer.setForeground(Color.WHITE);
        boutonDemarrer.setOpaque(true);
        boutonDemarrer.setBorderPainted(false);
        boutonDemarrer.setPreferredSize(new Dimension(0, 55));

        boutonDemarrer.addActionListener(e -> {
            strategieJoueur1 = configJ0.creerStrategie();
            strategieJoueur2 = configJ1.creerStrategie();
            int lignes   = (int) spinLignes.getValue();
            int colonnes = (int) spinColonnes.getValue();
            this.tailleCellule = Math.min(60, 600 / Math.max(lignes, colonnes));
            initialiserPartie(lignes, colonnes);
        });

        panneauSud.add(boutonDemarrer, BorderLayout.SOUTH);
        conteneurPrincipal.add(panneauSud, BorderLayout.SOUTH);

        setContentPane(conteneurPrincipal);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private class PanneauConfigJoueur extends JPanel {
        JComboBox<String> comboType;
        JSpinner spinnerParam;
        JLabel etiquetteParam;

        public PanneauConfigJoueur(int id, Color couleur) {
            setLayout(new GridLayout(0, 1, 5, 10));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(couleur, 3),
                    "JOUEUR " + id,
                    TitledBorder.LEFT, TitledBorder.TOP,
                    new Font("Segoe UI", Font.BOLD, 14)
            ));
            setBackground(Color.WHITE);

            comboType      = new JComboBox<>(new String[]{"Humain", "Aléatoire", "Glouton", "Minimax", "Alpha-Beta", "MCTS"});
            etiquetteParam = new JLabel("Paramètre :");
            spinnerParam   = new JSpinner(new SpinnerNumberModel(6, 1, 100000, 1));

            comboType.addActionListener(e -> mettreAJourChamps());
            add(new JLabel("Type de stratégie :"));
            add(comboType);
            add(etiquetteParam);
            add(spinnerParam);
            mettreAJourChamps();
        }

        private void mettreAJourChamps() {
            String selection = (String) comboType.getSelectedItem();
            boolean aParametre = !selection.equals("Humain")
                    && !selection.equals("Aléatoire")
                    && !selection.equals("Glouton");

            spinnerParam.setVisible(aParametre);
            etiquetteParam.setVisible(aParametre);

            if (selection.equals("MCTS")) {
                etiquetteParam.setText("Budget Temps (ms) :");
                spinnerParam.setValue(2000);
            } else {
                etiquetteParam.setText("Profondeur de calcul :");
                spinnerParam.setValue(6);
            }
        }

        public ActionStrategy creerStrategie() {
            String type = (String) comboType.getSelectedItem();
            int valeur  = (int) spinnerParam.getValue();
            return switch (type) {
                case "Aléatoire"  -> new RandomActionStrategy();
                case "Glouton"    -> new GloutonActionStrategy();
                case "Minimax"    -> new MinimaxActionStrategy(valeur);
                case "Alpha-Beta" -> new AlphaBetaActionStrategy(observateurAB, compteurNoeuds);
                case "MCTS"       -> MctsActionStrategy.withTimeBudget(valeur);
                default           -> null;
            };
        }
    }

    private void initialiserPartie(int lignes, int colonnes) {
        this.board           = new Board(lignes, colonnes);
        this.joueurCourantId = 0;

        panneauJeu = new PanneauPlateau();
        panneauJeu.setPreferredSize(new Dimension(
                colonnes * tailleCellule + MARGE * 2,
                lignes   * tailleCellule + MARGE * 2
        ));

        JPanel disposition = new JPanel(new BorderLayout());
        disposition.add(etiquetteStatut = creerBarreStatut(), BorderLayout.NORTH);
        disposition.add(panneauJeu,                           BorderLayout.CENTER);
        disposition.add(creerPanneauLateral(),                BorderLayout.WEST);

        setContentPane(disposition);
        pack();
        setLocationRelativeTo(null);
        rafraichirAffichage();
        verifierTourIA();
    }

    private JLabel creerBarreStatut() {
        JLabel etiquette = new JLabel("Match en cours...", SwingConstants.CENTER);
        etiquette.setOpaque(true);
        etiquette.setBackground(new Color(44, 62, 80));
        etiquette.setForeground(Color.WHITE);
        etiquette.setFont(new Font("Segoe UI", Font.BOLD, 18));
        etiquette.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return etiquette;
    }

    private JPanel creerPanneauLateral() {
        JPanel panneau = new JPanel();
        panneau.setLayout(new BoxLayout(panneau, BoxLayout.Y_AXIS));
        panneau.setPreferredSize(new Dimension(220, 0));
        panneau.setBackground(new Color(245, 245, 245));
        panneau.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        JPanel panneauStats = new JPanel(new GridLayout(0, 1, 5, 8));
        panneauStats.setBackground(Color.WHITE);
        panneauStats.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "ANALYSE TEMPS RÉEL",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        Font policeMonospace = new Font("Monospaced", Font.PLAIN, 12);
        lblNoeuds.setFont(policeMonospace);
        lblCoupures.setFont(policeMonospace);
        lblMcts.setFont(policeMonospace);
        lblDernierTemps.setFont(new Font("Monospaced", Font.BOLD, 13));

        panneauStats.add(lblNoeuds);
        panneauStats.add(lblCoupures);
        panneauStats.add(lblMcts);
        panneauStats.add(new JSeparator());
        panneauStats.add(lblDernierTemps);

        panneau.add(Box.createVerticalStrut(10));
        panneau.add(panneauStats);
        panneau.add(Box.createVerticalGlue());
        return panneau;
    }

    private void verifierTourIA() {
        ActionStrategy strategieCourante = (joueurCourantId == 0) ? strategieJoueur1 : strategieJoueur2;
        if (strategieCourante != null && !board.isFinished()) {
            Board copiePlateau = new Board(this.board);

            new Thread(() -> {
                long debut  = System.currentTimeMillis();
                Action action = strategieCourante.selectAction(copiePlateau, joueurCourantId);
                long fin    = System.currentTimeMillis();

                SwingUtilities.invokeLater(() -> {
                    mettreAJourAffichageStats(strategieCourante, fin - debut);
                    jouerAction(action);
                });
            }).start();
        }
    }

    private void mettreAJourAffichageStats(ActionStrategy strategie, long duree) {
        lblDernierTemps.setText("Dernier : " + duree + " ms");

        if (strategie instanceof AlphaBetaActionStrategy || strategie instanceof MinimaxActionStrategy) {
            lblNoeuds.setText("Nœuds : "    + String.format("%,d", compteurNoeuds.getCount()));
            lblCoupures.setText("Cuts : "   + observateurAB.getAlphaCutCount() + "α / " + observateurAB.getBetaCutCount() + "β");
            lblMcts.setText("Sims : -");
        } else if (strategie instanceof MctsActionStrategy mcts) {
            lblNoeuds.setText("Nœuds : -");
            lblCoupures.setText("Cuts : -");
            lblMcts.setText("Sims : " + String.format("%,d", mcts.getDernierTotalSimulations()));
        }
    }

    private void jouerAction(Action action) {
        if (action != null && board.isValid(action)) {
            int casesGagnees = board.apply(action, joueurCourantId);
            if (casesGagnees == 0) joueurCourantId = 1 - joueurCourantId;
            rafraichirAffichage();
            if (!board.isFinished()) verifierTourIA();
            else afficherGagnant();
        }
    }

    private void rafraichirAffichage() {
        String nomJoueur = (joueurCourantId == 0) ? "BLEU" : "ROUGE";
        etiquetteStatut.setText("TOUR DU JOUEUR " + nomJoueur);
        etiquetteStatut.setBackground(joueurCourantId == 0
                ? new Color(41, 128, 185)
                : new Color(192, 57, 43));
        panneauJeu.repaint();
    }

    private void afficherGagnant() {
        int scoreJ0 = board.getScore(0);
        int scoreJ1 = board.getScore(1);
        String resultat = (scoreJ0 > scoreJ1) ? "VICTOIRE BLEUE !"
                : (scoreJ1 > scoreJ0)  ? "VICTOIRE ROUGE !"
                : "ÉGALITÉ !";
        JOptionPane.showMessageDialog(this,
                resultat + "\nScore : " + scoreJ0 + " - " + scoreJ1,
                "Fin de match",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private class PanneauPlateau extends JPanel {

        public PanneauPlateau() {
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    ActionStrategy strategieCourante = (joueurCourantId == 0) ? strategieJoueur1 : strategieJoueur2;
                    if (strategieCourante == null) gererClicSouris(e.getX(), e.getY());
                }
            });
        }

        private void gererClicSouris(int x, int y) {
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    int px = MARGE + c * tailleCellule;
                    int py = MARGE + r * tailleCellule;

                    if (c < board.getCols() - 1) {
                        if (new Rectangle(px + 10, py - 10, tailleCellule - 20, 20).contains(x, y)) {
                            jouerAction(new Action(Action.Type.HORIZONTAL, r, c));
                            return;
                        }
                    }
                    if (r < board.getRows() - 1) {
                        if (new Rectangle(px - 10, py + 10, 20, tailleCellule - 20).contains(x, y)) {
                            jouerAction(new Action(Action.Type.VERTICAL, r, c));
                            return;
                        }
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            dessinerCases(g2);
            dessinerTraits(g2);
            dessinerPoints(g2);
        }

        private void dessinerCases(Graphics2D g2) {
            for (int r = 0; r < board.getRows() - 1; r++) {
                for (int c = 0; c < board.getCols() - 1; c++) {
                    int proprietaire = board.getBoxOwner(r, c);
                    if (proprietaire != -1) {
                        g2.setColor(proprietaire == 0
                                ? new Color(52, 152, 219, 120)
                                : new Color(231, 76, 60, 120));
                        g2.fillRect(
                                MARGE + c * tailleCellule + 2,
                                MARGE + r * tailleCellule + 2,
                                tailleCellule - 4,
                                tailleCellule - 4
                        );
                    }
                }
            }
        }

        private void dessinerTraits(Graphics2D g2) {
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    int x = MARGE + c * tailleCellule;
                    int y = MARGE + r * tailleCellule;

                    if (c < board.getCols() - 1) {
                        g2.setColor(board.isHEdgeSet(r, c)
                                ? new Color(44, 62, 80)
                                : new Color(236, 240, 241));
                        g2.drawLine(x + 4, y, x + tailleCellule - 4, y);
                    }
                    if (r < board.getRows() - 1) {
                        g2.setColor(board.isVEdgeSet(r, c)
                                ? new Color(44, 62, 80)
                                : new Color(236, 240, 241));
                        g2.drawLine(x, y + 4, x, y + tailleCellule - 4);
                    }
                }
            }
        }

        private void dessinerPoints(Graphics2D g2) {
            g2.setColor(new Color(44, 62, 80));
            int taillePt = Math.max(4, tailleCellule / 8);
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    g2.fillOval(
                            MARGE + c * tailleCellule - taillePt / 2,
                            MARGE + r * tailleCellule - taillePt / 2,
                            taillePt, taillePt
                    );
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {}
        SwingUtilities.invokeLater(DotsBoxesGUI::new);
    }
}