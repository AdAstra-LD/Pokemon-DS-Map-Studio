
package editor.state;

import java.util.ArrayList;

/**
 * @author Trifindo
 */
public class StateHandler {

    private static final int maxNumStates = 20;
    private int stateIndex;
    private boolean stateAdded = false;

    private final ArrayList<State> states;

    public StateHandler() {
        states = new ArrayList<>(maxNumStates + 1);
        stateIndex = 0;
    }

    public void addState(State state) {
        states.add(stateIndex, state);
        stateIndex++;
        stateAdded = true;
        if (stateIndex < states.size()) {
            states.subList(stateIndex, states.size()).clear();
        }
        if (states.size() > maxNumStates) {
            states.remove(0);
            stateIndex--;
        }
    }

    public State getPreviousState(State state) {
        if (!canGetPreviousState()) {
            return null;
        }
        if (stateAdded) {
            states.add(stateIndex, state);
        }
        stateAdded = false;
        stateIndex--;
        return states.get(stateIndex);
    }

    public State getNextState() {
        if (!canGetNextState()) {
            return null;
        }
        stateIndex++;
        return states.get(stateIndex);
    }

    public boolean canGetPreviousState() {
        return stateIndex > 0;
    }

    public boolean canGetNextState() {
        return stateIndex < states.size() - 1;
    }

    public int size() {
        return states.size();
    }

    public State getLastState() {
        if (stateIndex == 0) {
            return null;
        }
        return states.get(stateIndex - 1);
    }

}
