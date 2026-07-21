
package formats.animationeditor;

import javax.swing.SwingUtilities;

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
            SwingUtilities.invokeLater(animHandler::repaintDialog);

            try {
                Thread.sleep((long) ((Math.max(animHandler.getCurrentDelay(), 1) / 30.0f) * 1000));
            } catch (InterruptedException ex) {
                if (!running) {
                    return;
                }
            }

            animHandler.incrementFrameIndex();
        }
    }

    public void terminate() {
        this.running = false;
        interrupt();
    }

    public boolean isRunnning() {
        return running;
    }

}
