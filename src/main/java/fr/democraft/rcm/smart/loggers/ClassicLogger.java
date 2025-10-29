package fr.democraft.rcm.smart.loggers;
import fr.democraft.rcm.smart.SmartProvider;

public class ClassicLogger implements SmartLogger {
    public static String prefix = "SmartRC";

    @Override
    public void log(String message) {
        System.out.println("[" + prefix + "] " + message);
    }

    @Override
    public void debug(String message) {
        if (SmartProvider.DEBUG) {
            System.out.println("[DEBUG] [" + prefix + "] " + message);
        }
    }

    @Override
    public void error(String message) {
        System.out.println("[ERROR] [" + prefix + "] " + message);
    }
}
