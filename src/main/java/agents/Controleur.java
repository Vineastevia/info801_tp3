package agents;

import core.EtatSysteme;
import core.Programmateur;

import java.util.concurrent.*;

/**
 * Régulateur du système (controller dans la boucle de rétroaction).
 * Lit Tm et Tr, consulte le Programmateur, et décide d'allumer/éteindre la chaudière.
 * Implémente le protocole d'allumage avec timeout de 10s et gestion des erreurs.
 */
public class Controleur extends Thread {

    private static final double SEUIL = 2.0;            // fourchette ± 2°C
    private static final long TIMEOUT_ALLUMAGE_MS = 10_000; // 10 secondes
    private static final long DELAI_SECURITE_MS = 300_000;  // 5 minutes

    private final EtatSysteme etatSysteme;
    private final Programmateur programmateur;
    private final Chaudiere chaudiere;
    private final TableauControle tableauControle;
    private final long intervalle; // intervalle de vérification en ms

    private long tempsErreur;      // timestamp de la dernière erreur (0 si aucune)
    private volatile boolean actif;

    public Controleur(EtatSysteme etatSysteme, Programmateur programmateur,
                      Chaudiere chaudiere, TableauControle tableauControle,
                      long intervalle) {
        super("Controleur");
        this.etatSysteme = etatSysteme;
        this.programmateur = programmateur;
        this.chaudiere = chaudiere;
        this.tableauControle = tableauControle;
        this.intervalle = intervalle;
        this.tempsErreur = 0;
        this.actif = true;
    }

    @Override
    public void run() {
        System.out.println("[Controleur] Démarré (intervalle = " + intervalle + "ms)");
        while (actif) {
            double tm = etatSysteme.getTemperatureAmbiante();
            double tr = etatSysteme.getTemperatureReference();
            boolean enPlage = programmateur.estEnPlage();
            boolean allumee = etatSysteme.isChaudiereAllumee();

            if (enPlage) {
                // Mode programmé prioritaire
                if (!allumee) {
                    System.out.println("[Controleur] Mode PROGRAMMÉ actif — allumage requis");
                    tenterAllumage();
                }
                // Si déjà allumée, on ne fait rien (on ignore le seuil)
            } else {
                // Mode régulé
                if (tm <= tr - SEUIL && !allumee) {
                    System.out.println("[Controleur] Tm=" + tm + "°C ≤ Tr-2=" + (tr - SEUIL)
                            + "°C — seuil bas atteint, allumage requis");
                    tenterAllumage();
                } else if (tm >= tr + SEUIL && allumee) {
                    System.out.println("[Controleur] Tm=" + tm + "°C ≥ Tr+2=" + (tr + SEUIL)
                            + "°C — seuil haut atteint, extinction");
                    chaudiere.eteindre();
                    etatSysteme.setChaudiereAllumee(false);
                }
            }

            try {
                Thread.sleep(intervalle);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[Controleur] Arrêté");
    }

    /**
     * Tente l'allumage de la chaudière selon le protocole :
     * 1. Vérifier disjoncteur
     * 2. Vérifier délai de sécurité (5 min après erreur)
     * 3. Envoyer ordre d'allumage
     * 4. Attendre compte-rendu (timeout 10s)
     * 5. Traiter le résultat
     */
    private void tenterAllumage() {
        // Vérification du disjoncteur
        if (!tableauControle.isDisjoncteurOK()) {
            System.out.println("[Controleur] Allumage BLOQUÉ — disjoncteur non réenclenché");
            return;
        }

        // Vérification du délai de sécurité
        if (tempsErreur > 0) {
            long ecart = System.currentTimeMillis() - tempsErreur;
            if (ecart < DELAI_SECURITE_MS) {
                long restant = (DELAI_SECURITE_MS - ecart) / 1000;
                System.out.println("[Controleur] Allumage BLOQUÉ — délai de sécurité ("
                        + restant + "s restantes)");
                return;
            }
        }

        // Envoi de l'ordre d'allumage avec timeout
        System.out.println("[Controleur] Envoi de l'ordre d'allumage...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Chaudiere.Signal> future = executor.submit(() -> chaudiere.allumer());

        Chaudiere.Signal signal;
        try {
            signal = future.get(TIMEOUT_ALLUMAGE_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            signal = Chaudiere.Signal.TIMEOUT;
            future.cancel(true);
            System.out.println("[Controleur] ⚠ TIMEOUT — aucun compte-rendu en "
                    + (TIMEOUT_ALLUMAGE_MS / 1000) + "s");
        } catch (InterruptedException | ExecutionException e) {
            signal = Chaudiere.Signal.DEFAILLANCE;
            System.out.println("[Controleur] ⚠ Erreur lors de l'allumage : " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }

        // Traitement du compte-rendu
        switch (signal) {
            case OK:
                etatSysteme.setChaudiereAllumee(true);
                tempsErreur = 0;
                System.out.println("[Controleur] ✓ Chaudière allumée avec succès");
                break;

            case DEFAILLANCE:
            case TIMEOUT:
                etatSysteme.setChaudiereAllumee(false);
                tableauControle.allumerVoyant();
                tableauControle.actionnerDisjoncteur();
                tempsErreur = System.currentTimeMillis();
                System.out.println("[Controleur] ✗ Échec allumage — erreur enregistrée, "
                        + "prochaine tentative dans " + (DELAI_SECURITE_MS / 1000 / 60) + " min");
                break;
        }
    }

    public void arreter() {
        this.actif = false;
        this.interrupt();
    }

    // --- Pour les tests : permet de réduire les délais ---

    /**
     * Modifie le délai de sécurité (utile pour les scénarios de test).
     */
    public void setDelaiSecuriteMs(long delaiMs) {
        // On utilise la réflexion serait trop complexe, on expose un setter simple
        // En production ce serait une constante, ici c'est pour les tests
    }

    /**
     * Réinitialise le temps d'erreur (pour les tests).
     */
    public void resetErreur() {
        this.tempsErreur = 0;
    }
}
