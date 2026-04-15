package agents;

import core.EtatSysteme;

/**
 * Capteur du système (sensor dans la boucle de rétroaction).
 * Mesure périodiquement la température ambiante et l'écrit dans EtatSysteme.
 *
 * La simulation modélise l'effet de la chaudière sur la température :
 * - Chaudière allumée → Tm augmente progressivement
 * - Chaudière éteinte → Tm diminue progressivement (refroidissement naturel)
 */
public class Thermostat extends Thread {

    private final EtatSysteme etatSysteme;
    private final long intervalle; // en ms
    private final double variationParTick; // variation de température par cycle
    private volatile boolean actif;

    public Thermostat(EtatSysteme etatSysteme, long intervalle, double variationParTick) {
        super("Thermostat");
        this.etatSysteme = etatSysteme;
        this.intervalle = intervalle;
        this.variationParTick = variationParTick;
        this.actif = true;
    }

    @Override
    public void run() {
        System.out.println("[Thermostat] Démarré (intervalle = " + intervalle + "ms)");
        while (actif) {
            double tm = etatSysteme.getTemperatureAmbiante();

            if (etatSysteme.isChaudiereAllumee()) {
                tm += variationParTick; // chauffage
            } else {
                tm -= variationParTick; // refroidissement naturel
            }

            // Arrondi à 1 décimale
            tm = Math.round(tm * 10.0) / 10.0;

            etatSysteme.setTemperatureAmbiante(tm);
            System.out.println("[Thermostat] Tm = " + tm + "°C"
                    + (etatSysteme.isChaudiereAllumee() ? " (chauffage)" : " (refroidissement)"));

            try {
                Thread.sleep(intervalle);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[Thermostat] Arrêté");
    }

    public void arreter() {
        this.actif = false;
        this.interrupt();
    }
}