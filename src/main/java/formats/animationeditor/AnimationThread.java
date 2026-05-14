
package formats.animationeditor;

/**
 * @author Trifindo
 */
public class AnimationThread extends Thread {

    private AnimationHandler animHandler;
    private volatile boolean running = true;

    public AnimationThread(AnimationHandler animHandler) {
        this.animHandler = animHandler;
    }

    @Override
    public void run() {
        while (running) {
            animHandler.repaintDialog();

            try {
                Thread.sleep((long) ((Math.max(animHandler.getCurrentDelay(), 1) / 30.0f) * 1000));
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            animHandler.incrementFrameIndex();
        }
    }

    public void terminate() {
        this.running = false;
    }

    public boolean isRunnning() {
        return running;
    }

}
