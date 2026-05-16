package Gastly.fr.mclauncher.data;

public class TimeCount extends Thread {
    private static int count = 0;

    public TimeCount(){}

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(60000); // Attendre 1 minute
                count += 1; // Augmenter le compteur
            }
        } catch (InterruptedException e) {
            // Le thread a été interrompu, on arrête proprement
            Thread.currentThread().interrupt();
        }
    }
    public int Stop(){
        int TimeCount = count;
        count = 0;
        this.interrupt();

        return TimeCount;
    }
}
