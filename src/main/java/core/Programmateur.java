package core;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère les plages horaires où la chaudière doit fonctionner (mode programmé).
 * Le mode programmé est prioritaire sur le mode régulé.
 */
public class Programmateur {

    private final List<PlageHoraire> plages;

    public Programmateur() {
        this.plages = new ArrayList<>();
    }

    public synchronized void ajouterPlage(LocalTime debut, LocalTime fin) {
        plages.add(new PlageHoraire(debut, fin));
        System.out.println("[Programmateur] Plage ajoutée : " + debut + " → " + fin);
    }

    /**
     * Vérifie si l'heure courante se trouve dans une plage programmée.
     */
    public synchronized boolean estEnPlage() {
        LocalTime maintenant = LocalTime.now();
        return estEnPlage(maintenant);
    }

    /**
     * Vérifie si une heure donnée se trouve dans une plage programmée.
     * (Utilisé pour les tests avec une heure simulée.)
     */
    public synchronized boolean estEnPlage(LocalTime heure) {
        for (PlageHoraire plage : plages) {
            if (!heure.isBefore(plage.debut) && heure.isBefore(plage.fin)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void viderPlages() {
        plages.clear();
    }

    /**
     * Représente une plage horaire [debut, fin[.
     */
    private static class PlageHoraire {
        final LocalTime debut;
        final LocalTime fin;

        PlageHoraire(LocalTime debut, LocalTime fin) {
            this.debut = debut;
            this.fin = fin;
        }
    }
}