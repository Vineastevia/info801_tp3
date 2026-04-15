package agents;

/**
 * Gère le voyant d'erreur et le disjoncteur.
 * Le disjoncteur doit être réenclenché manuellement après une erreur.
 */
public class TableauControle {

    private boolean voyantAllume;
    private boolean disjoncteurOK;

    public TableauControle() {
        this.voyantAllume = false;
        this.disjoncteurOK = true;
    }

    public synchronized void allumerVoyant() {
        this.voyantAllume = true;
        System.out.println("[TableauControle] ⚠ Voyant d'erreur ALLUMÉ");
    }

    public synchronized void eteindreVoyant() {
        this.voyantAllume = false;
        System.out.println("[TableauControle] Voyant d'erreur éteint");
    }

    public synchronized boolean isVoyantAllume() {
        return voyantAllume;
    }

    public synchronized void actionnerDisjoncteur() {
        this.disjoncteurOK = false;
        System.out.println("[TableauControle] ⚠ Disjoncteur DÉCLENCHÉ");
    }

    /**
     * Réenclenchement manuel du disjoncteur par l'utilisateur.
     * Éteint également le voyant.
     */
    public synchronized void reenclencher() {
        this.disjoncteurOK = true;
        this.voyantAllume = false;
        System.out.println("[TableauControle] ✓ Disjoncteur réenclenché manuellement");
    }

    public synchronized boolean isDisjoncteurOK() {
        return disjoncteurOK;
    }
}