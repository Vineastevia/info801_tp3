import agents.*;
import core.*;

import java.time.LocalTime;

/**
 * Point d'entrée de la simulation.
 * Exécute les 4 scénarios d'intégration définis dans le compte rendu.
 */
public class App {

    private static final double PRIX_INITIAL_TM = 20.0;
    private static final double PRIX_INITIAL_TR = 20.0;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=============================================================");
        System.out.println("  SCÉNARIO 1 — Régulation normale (seuil bas)");
        System.out.println("=============================================================\n");
        scenario1();

        Thread.sleep(1000);

        System.out.println("\n=============================================================");
        System.out.println("  SCÉNARIO 2 — Défaillance et reprise");
        System.out.println("=============================================================\n");
        scenario2();

        Thread.sleep(1000);

        System.out.println("\n=============================================================");
        System.out.println("  SCÉNARIO 3 — Mode programmé prioritaire");
        System.out.println("=============================================================\n");
        scenario3();

        Thread.sleep(1000);

        System.out.println("\n=============================================================");
        System.out.println("  SCÉNARIO 4 — Timeout du compte-rendu");
        System.out.println("=============================================================\n");
        scenario4();
    }

    /**
     * Scénario 1 — Régulation normale (seuil bas)
     * Tm descend à 18°C (Tr=20), le contrôleur allume la chaudière.
     * Tm remonte jusqu'à 22°C (Tr+2), le contrôleur éteint la chaudière.
     */
    private static void scenario1() throws InterruptedException {
        EtatSysteme etat = new EtatSysteme(18.0, 20.0); // Tm=18, Tr=20
        Programmateur prog = new Programmateur();
        Chaudiere chaudiere = new Chaudiere();
        TableauControle tableau = new TableauControle();

        Thermostat thermostat = new Thermostat(etat, 300, 0.5);
        Controleur controleur = new Controleur(etat, prog, chaudiere, tableau, 500);

        thermostat.start();
        controleur.start();

        // Laisser tourner le temps que Tm passe de 18 → 22 et que la chaudière s'éteigne
        Thread.sleep(8000);

        controleur.arreter();
        thermostat.arreter();
        controleur.join();
        thermostat.join();

        System.out.println("\n[Scénario 1] Tm finale = " + etat.getTemperatureAmbiante()
                + "°C | Chaudière = " + (etat.isChaudiereAllumee() ? "allumée" : "éteinte"));
    }

    /**
     * Scénario 2 — Défaillance et reprise
     * La chaudière simule une défaillance → voyant + disjoncteur.
     * Après réenclenchement, l'allumage réussit.
     */
    private static void scenario2() throws InterruptedException {
        EtatSysteme etat = new EtatSysteme(17.0, 20.0); // Tm=17, seuil bas atteint
        Programmateur prog = new Programmateur();
        Chaudiere chaudiere = new Chaudiere();
        TableauControle tableau = new TableauControle();

        // Simuler une défaillance
        chaudiere.setSimulerDefaillance(true);

        Controleur controleur = new Controleur(etat, prog, chaudiere, tableau, 500);
        controleur.start();

        // Laisser le contrôleur détecter le seuil bas et tenter l'allumage (qui échoue)
        Thread.sleep(2000);

        System.out.println("\n--- Voyant allumé ? " + tableau.isVoyantAllume()
                + " | Disjoncteur OK ? " + tableau.isDisjoncteurOK());

        // L'utilisateur réenclenche le disjoncteur
        System.out.println("\n--- L'utilisateur réenclenche le disjoncteur ---");
        tableau.reenclencher();

        // Désactiver la défaillance et réinitialiser l'erreur pour le test
        chaudiere.setSimulerDefaillance(false);
        controleur.resetErreur();

        // Laisser le contrôleur retenter l'allumage
        Thread.sleep(2000);

        controleur.arreter();
        controleur.join();

        System.out.println("\n[Scénario 2] Chaudière = "
                + (etat.isChaudiereAllumee() ? "allumée (reprise OK)" : "éteinte (échec)"));
    }

    /**
     * Scénario 3 — Mode programmé prioritaire
     * Une plage horaire couvre l'heure actuelle → la chaudière reste allumée
     * même si Tm dépasse Tr+2.
     */
    private static void scenario3() throws InterruptedException {
        EtatSysteme etat = new EtatSysteme(20.0, 20.0);
        Programmateur prog = new Programmateur();
        Chaudiere chaudiere = new Chaudiere();
        TableauControle tableau = new TableauControle();

        // Programmer une plage qui couvre l'heure actuelle
        LocalTime maintenant = LocalTime.now();
        prog.ajouterPlage(maintenant.minusMinutes(5), maintenant.plusMinutes(5));

        Thermostat thermostat = new Thermostat(etat, 300, 0.5);
        Controleur controleur = new Controleur(etat, prog, chaudiere, tableau, 500);

        thermostat.start();
        controleur.start();

        // Laisser Tm monter au-delà de Tr+2 pendant la plage programmée
        Thread.sleep(6000);

        double tmPendantPlage = etat.getTemperatureAmbiante();
        boolean allumePendantPlage = etat.isChaudiereAllumee();

        System.out.println("\n--- Pendant plage programmée : Tm=" + tmPendantPlage
                + "°C | Chaudière = " + (allumePendantPlage ? "allumée (priorité plage)" : "éteinte"));

        // Retirer la plage pour revenir en mode régulé
        System.out.println("--- Fin de la plage programmée (retour mode régulé) ---");
        prog.viderPlages();

        // Laisser le mode régulé éteindre la chaudière si Tm > Tr+2
        Thread.sleep(3000);

        controleur.arreter();
        thermostat.arreter();
        controleur.join();
        thermostat.join();

        System.out.println("\n[Scénario 3] Tm finale = " + etat.getTemperatureAmbiante()
                + "°C | Chaudière = " + (etat.isChaudiereAllumee() ? "allumée" : "éteinte (mode régulé)"));
    }

    /**
     * Scénario 4 — Timeout du compte-rendu
     * La chaudière ne répond pas dans les 10s → traitement identique à une défaillance.
     * (Pour le test, on simule un timeout rapide via le signal TIMEOUT.)
     */
    private static void scenario4() throws InterruptedException {
        EtatSysteme etat = new EtatSysteme(17.0, 20.0); // seuil bas atteint
        Programmateur prog = new Programmateur();
        Chaudiere chaudiere = new Chaudiere();
        TableauControle tableau = new TableauControle();

        // Simuler un timeout
        chaudiere.setSimulerTimeout(true);

        Controleur controleur = new Controleur(etat, prog, chaudiere, tableau, 500);
        controleur.start();

        // Laisser le contrôleur tenter l'allumage (timeout)
        Thread.sleep(2000);

        controleur.arreter();
        controleur.join();

        System.out.println("\n[Scénario 4] Voyant = " + (tableau.isVoyantAllume() ? "ALLUMÉ" : "éteint")
                + " | Disjoncteur = " + (tableau.isDisjoncteurOK() ? "OK" : "DÉCLENCHÉ")
                + " | Chaudière = " + (etat.isChaudiereAllumee() ? "allumée" : "éteinte"));
    }
}