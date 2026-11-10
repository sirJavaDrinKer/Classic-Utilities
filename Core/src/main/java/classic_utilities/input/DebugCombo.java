package classic_utilities.input;

public final class DebugCombo {
    private boolean keyWasDown;
    private boolean undoPending;
    private boolean debugBeforeF3;

    public boolean pollFrame(boolean f3Down, boolean keyDown) {
        boolean fired = f3Down && keyDown && !keyWasDown;
        keyWasDown = keyDown;
        if(fired) undoPending = true;
        return fired;
    }

    public boolean pollTick(boolean f3Down, boolean debugScreenShown) {
        if(undoPending) {
            undoPending = false;
            return true;
        }

        if(!f3Down) debugBeforeF3 = debugScreenShown;
        return false;
    }

    public boolean debugToRestore() {
        return debugBeforeF3;
    }
}
