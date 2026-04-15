package core;

/**
 * Dépôt central partagé du système.
 * Stocke Tm (température ambiante), Tr (température de référence)
 * et l'état de la chaudière. Toutes les méthodes sont synchronized.
 */
public class EtatSysteme {

    private double temperatureAmbiante;  // Tm
    private double temperatureReference; // Tr
    private boolean chaudiereAllumee;

    public EtatSysteme(double tmInitiale, double trInitiale) {
        this.temperatureAmbiante = tmInitiale;
        this.temperatureReference = trInitiale;
        this.chaudiereAllumee = false;
    }

    public synchronized double getTemperatureAmbiante() {
        return temperatureAmbiante;
    }

    public synchronized void setTemperatureAmbiante(double tm) {
        this.temperatureAmbiante = tm;
    }

    public synchronized double getTemperatureReference() {
        return temperatureReference;
    }

    public synchronized void setTemperatureReference(double tr) {
        if (tr < 5 || tr > 35) {
            System.out.println("[EtatSysteme] Température de référence hors limites [5, 35] : " + tr);
            return;
        }
        this.temperatureReference = tr;
        System.out.println("[EtatSysteme] Nouvelle température de référence : " + tr + "°C");
    }

    public synchronized boolean isChaudiereAllumee() {
        return chaudiereAllumee;
    }

    public synchronized void setChaudiereAllumee(boolean allumee) {
        this.chaudiereAllumee = allumee;
    }
}