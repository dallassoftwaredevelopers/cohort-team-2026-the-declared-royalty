package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.util.Map;

@Service
public class OpenApiToMapService {

    public Map<String, Object> convertOpenApiToMap() {

        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));
        String filename = "OpenAiResponse";

        // Using the current Thread's classloader removes the need for any specific class name reference
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("openapi/components/schemas/" + filename + ".yaml")) {

            if (inputStream == null) {
                System.err.println("Error: " + filename + ".yaml file not found in src/main/resources/openapi/components/schemas/");
                return null;
            }

            // SnakeYAML parses the file directly into native Java Maps and Lists
            return yaml.load(inputStream);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
