package fr.democraft.rcm.smart.loggers;

public interface SmartLogger {
    void log(String message);
    void debug(String message);
    void error(String message);
}

