package fr.democraft.rcm.smart.providers;

// ProviderConfig.java
import java.util.List;

public class ProviderConfig {
    private String id;
    private List<String> include;
    private List<String> exclude;
    private int process;
    private int ram;

    // Default constructor for YAML mapping
    public ProviderConfig() {}

    public ProviderConfig(String id, List<String> include, List<String> exclude, int process, int ram) {
        this.id = id;
        this.include = include;
        this.exclude = exclude;
        this.process = process;
        this.ram = ram;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getInclude() { return include; }
    public void setInclude(List<String> include) { this.include = include; }

    public List<String> getExclude() { return exclude; }
    public void setExclude(List<String> exclude) { this.exclude = exclude; }

    public int getProcess() { return process; }
    public void setProcess(int process) { this.process = process; }

    public int getRam() { return ram; }
    public void setRam(int ram) { this.ram = ram; }

    @Override
    public String toString() {
        return "ProviderConfig{id='" + id + "', include=" + include +
                ", exclude=" + exclude + ", process=" + process + ", ram=" + ram + "}";
    }
}
