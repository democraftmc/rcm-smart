package fr.democraft.rcm.smart.providers;

// ProviderSelectionSystem.java
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProviderSelectionSystem {
    private List<ProviderState> providerStates;

    public ProviderSelectionSystem() {
        this.providerStates = new ArrayList<>();
        loadProvidersFromConfig();
    }

    @SuppressWarnings("unchecked")
    private void loadProvidersFromConfig() {
        Yaml yaml = new Yaml();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("servers.yml")) {
            if (inputStream == null) {
                throw new RuntimeException("server.yml not found in classpath");
            }

            // Parse the YAML file
            Map<String, Object> yamlData = yaml.load(inputStream);
            List<Map<String, Object>> providers = (List<Map<String, Object>>) yamlData.get("providers");

            if (providers == null) {
                throw new RuntimeException("No 'providers' section found in server.yml");
            }

            // Convert YAML data to ProviderConfig objects
            for (Map<String, Object> providerData : providers) {
                ProviderConfig config = new ProviderConfig();
                config.setId((String) providerData.get("id"));
                config.setInclude((List<String>) providerData.get("include"));
                config.setExclude((List<String>) providerData.get("exclude"));
                config.setProcess((Integer) providerData.get("process"));
                config.setRam((Integer) providerData.get("ram"));

                providerStates.add(new ProviderState(config));
            }

            System.out.println("Loaded " + providerStates.size() + " providers from configuration.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to load provider configuration", e);
        }
    }

    public String selectProvider(String familyName, int ramAmount) {
        System.out.println("Selecting provider for family: " + familyName + " with RAM: " + ramAmount + "MB");

        for (ProviderState provider : providerStates) {
            if (provider.canAcceptFamily(familyName, ramAmount)) {
                provider.assignFamily(ramAmount);
                System.out.println("Selected provider: " + provider.getConfig().getId());
                System.out.println("Updated state: " + provider);
                return provider.getConfig().getId();
            }
        }

        System.out.println("No suitable provider found for family: " + familyName);
        return null;
    }
}
