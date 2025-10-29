package fr.democraft.rcm.smart.providers;

// ProviderState.java
public class ProviderState {
    private final ProviderConfig config;
    private int currentProcessCount;
    private int currentRamUsage;

    public ProviderState(ProviderConfig config) {
        this.config = config;
        this.currentProcessCount = 0;
        this.currentRamUsage = 0;
    }

    public boolean canAcceptFamily(String familyName, int requiredRam) {
        // Check include list
        if (config.getInclude() != null && !config.getInclude().isEmpty() &&
                !config.getInclude().contains(familyName)) {
            return false;
        }

        // Check exclude list
        if (config.getExclude() != null && config.getExclude().contains(familyName)) {
            return false;
        }

        // Check process limit
        if (currentProcessCount >= config.getProcess()) {
            return false;
        }

        // Check RAM availability
        if (currentRamUsage + requiredRam > config.getRam()) {
            return false;
        }

        return true;
    }

    public void assignFamily(int requiredRam) {
        currentProcessCount++;
        currentRamUsage += requiredRam;
    }

    // Getters
    public ProviderConfig getConfig() { return config; }
    public int getCurrentProcessCount() { return currentProcessCount; }
    public int getCurrentRamUsage() { return currentRamUsage; }

    @Override
    public String toString() {
        return "ProviderState{id='" + config.getId() + "', currentProcess=" + currentProcessCount +
                ", currentRam=" + currentRamUsage + "/" + config.getRam() + "}";
    }
}
