package agents;

/**
 * Actionneur du système. Simule l'ignition et la mise en route du moteur.
 * Envoie un signal (OK ou DEFAILLANCE) au contrôleur après allumage.
 */
public class Chaudiere {

    public enum Signal { OK, DEFAILLANCE, TIMEOUT }

    private boolean allumee;
    private boolean simulerDefaillance;
    private boolean simulerTimeout;

    public Chaudiere() {
        this.allumee = false;
        this.simulerDefaillance = false;
        this.simulerTimeout = false;
    }

    /**
     * Tente l'allumage de la chaudière (synchronized).
     * Retourne un signal OK ou DEFAILLANCE.
     * Si simulerTimeout est activé, bloque indéfiniment (le contrôleur gère le timeout).
     */
    public synchronized Signal allumer() {
        System.out.println("[Chaudiere] Ordre d'allumage reçu — ignition en cours...");

        // Simuler le temps d'ignition + mise en route moteur
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (simulerTimeout) {
            System.out.println("[Chaudiere] Pas de réponse (simulation timeout)...");
            // On retourne quand même TIMEOUT pour ne pas bloquer indéfiniment
            // Le contrôleur gère le timeout via un mécanisme séparé
            return Signal.TIMEOUT;
        }

        if (simulerDefaillance) {
            System.out.println("[Chaudiere] ✗ DÉFAILLANCE lors de l'ignition !");
            this.allumee = false;
            return Signal.DEFAILLANCE;
        }

        this.allumee = true;
        System.out.println("[Chaudiere] ✓ Ignition réussie — chaudière en marche");
        return Signal.OK;
    }

    public synchronized void eteindre() {
        this.allumee = false;
        System.out.println("[Chaudiere] Chaudière éteinte");
    }

    public synchronized boolean isAllumee() {
        return allumee;
    }

    // --- Méthodes de simulation pour les scénarios de test ---

    public void setSimulerDefaillance(boolean simuler) {
        this.simulerDefaillance = simuler;
    }

    public void setSimulerTimeout(boolean simuler) {
        this.simulerTimeout = simuler;
    }
}