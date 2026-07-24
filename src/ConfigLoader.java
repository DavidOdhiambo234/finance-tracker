import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties properties = new Properties();

    static {
        try {
            String[] paths = {
                "config.properties",
                System.getProperty("user.dir") + "/config.properties",
                System.getProperty("config.path")
            };

            for (String path : paths) {
                if (path != null) {
                    try (FileInputStream fis = new FileInputStream(path)) {
                        properties.load(fis);
                        System.out.println("? Config loaded from: " + path);
                        break;
                    } catch (IOException ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("?? Failed to load config: " + e.getMessage());
        }
    }

    public static String getProperty(String key) {
        // FIRST: Try environment variables
        String envKey = key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // SECOND: Fall back to properties file
        String value = properties.getProperty(key);
        return value != null ? value.trim() : "";
    }
}
