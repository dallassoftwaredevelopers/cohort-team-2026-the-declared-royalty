package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.*;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiRequestService {

    private final OpenAIClient client;

    public AiRequestService(OpenAIClient client) {
        this.client = client;
    }

    public String testConnection() {
        Map<String, Object> schema =
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string"),
                                "date", Map.of("type", "string"),
                                "participants", Map.of("type", "array", "items", Map.of("type", "string"))),
                        "required", List.of("name", "date", "participants"),
                        "additionalProperties", false
                );

        ResponseCreateParams params =
                ResponseCreateParams.builder()
                        .model("gpt-6-astra")
                        .inputOfResponse(
                                List.of(
                                        ResponseInputItem.ofEasyInputMessage(
                                                EasyInputMessage.builder()
                                                        .role(EasyInputMessage.Role.SYSTEM)
                                                        .content("Extract the event information.")
                                                        .build()),
                                        ResponseInputItem.ofEasyInputMessage(
                                                EasyInputMessage.builder()
                                                        .role(EasyInputMessage.Role.USER)
                                                        .content("Alice and Bob are going to a science fair on Friday.")
                                                        .build())))
                        .text(
                                ResponseTextConfig.builder()
                                        .format(
                                                ResponseFormatTextJsonSchemaConfig.builder()
                                                        .name("event")
                                                        .strict(true)
                                                        .schema(
                                                                JsonValue.from(schema)
                                                                        .convert(ResponseFormatTextJsonSchemaConfig.Schema.class))
                                                        .build())
                                        .build())
                        .build();
        return client.responses().create(params).output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(text -> text.text())
                .collect(Collectors.joining("\n"));
    }

    public String analyzeInvoice() {
        String promptFilePath = "/llm.resources/llm_prompt.txt";
        String schemaFilePath = "/llm.resources/llm_request.json";
        String invoiceName = "invoices/alder-creek-bill.pdf";

        JSONObject extractedValuesStructure = getJsonFromFile("/llm.resources/extracted_values_list.json");

        ResponseCreateParams requestParams = getRequestParams(promptFilePath, invoiceName, schemaFilePath, extractedValuesStructure);
        return client.responses().create(requestParams).output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(text -> text.text())
                .collect(Collectors.joining("\n"));
    }



//    https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses&lang=java
//    generates request parameters for the AI model based on the provided prompt and schema file paths
    private ResponseCreateParams getRequestParams(String promptFilePath, String invoiceName, String schemaFilePath, JSONObject extractedValuesStructure) {
        return ResponseCreateParams.builder()
                .model("gpt-6-astra")
                    .inputOfResponse(
                            List.of(
//                                    provides the prompt to the AI model for processing
                                    ResponseInputItem.ofEasyInputMessage(
                                            EasyInputMessage.builder()
                                                    .role(EasyInputMessage.Role.SYSTEM)
                                                    .content(getPrompt(promptFilePath))
                                                    .build()),
//                                    provides the byte data of the invoice as a base64-encoded data URL to the AI model for processing
                                    ResponseInputItem.ofEasyInputMessage(
                                            EasyInputMessage.builder()
                                                    .role(EasyInputMessage.Role.USER)
                                                    // Pass structured blocks instead of a single text string
                                                    .content(EasyInputMessage.Content.ofResponseInputMessageContentList(List.of(
                                                                    // Define the file block with filename and data URI
                                                                    ResponseInputContent.ofInputFile(
                                                                            ResponseInputFile.builder()
                                                                                    .filename(invoiceName + ".pdf")
                                                                                    .fileData(getInvoiceBase64DataUrl(invoiceName)) // Ensure this starts with "data:application/pdf;base64,"
//                                                                                    .detail(ResponseInputFile.Detail.HIGH) // Optional: HIGH details for text-heavy invoices
                                                                                    .build()
                                                                    ))))
                                                    .build())))
//                provides the expected response format to the AI model, including the JSON schema for validation
                    .text(
                            ResponseTextConfig.builder()
                                    .format(
                                            ResponseFormatTextJsonSchemaConfig.builder()
                                                    .name("invoiceValues")
                                                    .strict(true)
                                                    .schema(
                                                            JsonValue.from(getSchema(schemaFilePath, extractedValuesStructure))
                                                                    .convert(ResponseFormatTextJsonSchemaConfig.Schema.class))
                                                    .build())
                                    .build())
                    .build();
    }

//    generates a JSON schema for the LLM response based on the sample JSON provided at the file path
    private Map<String, Object> getSchema(String jsonStructureFilePath, JSONObject extractedValuesJson) {
        Map<String, Object> properties = getSchemaFromJsonObject(getJsonFromFile(jsonStructureFilePath), extractedValuesJson);

        return Map.of(
                "type", "object",
                "properties", properties,
                "required", properties.keySet(),
                "additionalProperties", false
        );
    }

//    recursively generates a JSON schema for the LLM response based on the sample JSON Objects provided
//    structureJsonObject: the sample JSON structure that defines the overall structure for the returned JSON. Defined in "src/main/resources/llm.resources/llm_request.json"
//    extractedValuesJson: the sample JSON structure that defines the desired values to be extracted and their types. Defined in "src/main/resources/llm.resources/extracted_values.json" but can also be received from the frontend
    private Map<String, Object> getSchemaFromJsonObject(JSONObject structureJsonObject, JSONObject extractedValuesJson) {
        Map<String, Object> properties = new HashMap<>();
        for (String field : structureJsonObject.keySet()) {
            Object value = structureJsonObject.get(field);
            String type = determineJsonType(value);
            if(type.equals("object") && field.equals("extracted_values")) {
                Map<String, Object> nestedProperties = populateExtractedValues(extractedValuesJson);
                properties.put(field, Map.of(
                        "type", "object",
                        "properties", nestedProperties,
                        "additionalProperties", false,
                        "required", nestedProperties.keySet()
                ));
            } else if(type.equals("object") && value instanceof JSONObject nestedObject) {
                Map<String, Object> nestedProperties = getSchemaFromJsonObject(nestedObject, new JSONObject());
                properties.put(field, Map.of(
                        "type", "object",
                        "properties", nestedProperties,
                        "additionalProperties", false,
                        "required", nestedProperties.keySet()
                ));
            } else {
                properties.put(field, Map.of("type", type));
            }
        }
        return properties;
    }

    private Map<String, Object> populateExtractedValues(JSONObject extractedValuesJson) {
        Map<String, Object> properties = new HashMap<>();
        for (String field : extractedValuesJson.keySet()) {
            Object value = extractedValuesJson.get(field);
            String type = determineJsonType(value);
            properties.put(field, Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "value", Map.of("type", type),
                            "confidence", Map.of("type", "number")
                    ),
                    "additionalProperties", false,
                    "required", List.of("value", "confidence")
            ));
        }
        return properties;
    }

//    reads a JSON file from the specified file path and returns it as a JSONObject
    private JSONObject getJsonFromFile(String filePath) {
        try (var reader = new InputStreamReader(
                getClass().getResourceAsStream(filePath),
                StandardCharsets.UTF_8)) {
            if (reader == null) {
                throw new IllegalArgumentException("JSON file not found: " + filePath);
            }
            Object parsed = JSONValue.parse(reader);
            if (!(parsed instanceof JSONObject)) {
                throw new IllegalArgumentException("JSON root is not an object: " + filePath);
            }
            return (JSONObject) parsed;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON from file: " + filePath, e);
        }
    }

//    determines the JSON type of a given value and returns it as a string
    private String determineJsonType(Object value) {
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof JSONObject || value instanceof Map) {
            return "object";
        }
        if (value instanceof Iterable || value.getClass().isArray()) {
            return "array";
        }
        return "string";
    }

//    reads a prompt from the specified file path and returns it as a string
//    expected prompt filepath: "src/main/resources/llm.resources/llm_prompt.txt"
    private String getPrompt(String filePath){
        try (var reader = new InputStreamReader(
                getClass().getResourceAsStream(filePath),
                StandardCharsets.UTF_8)) {
            if (reader == null) {
                throw new IllegalArgumentException("Prompt file not found: " + filePath);
            }
            StringBuilder content = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                content.append((char) ch);
            }
            return content.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read prompt from file: " + filePath, e);
        }
    }

//    reads an invoice file from MinIO storage using getInvoiceBase64 and returns its byte data as a base64-encoded data URL
    private String getInvoiceBase64DataUrl(String filename){
        return "data:application/pdf;base64," + java.util.Base64.getEncoder().encodeToString(getInvoiceBase64(filename));
    }

//    reads an invoice file from MinIO storage and returns its byte data as a byte array
    private byte[] getInvoiceBase64(String filename){

        try(
                InputStream stream = MinioClient.builder()
                        .endpoint("http://minio:9000")
                        .credentials("minio_user","minio_password")
                        .build()
                        .getObject(
                                GetObjectArgs.builder()
                                        .bucket("invoices")
                                        .object(filename)
                                        .build()
                        )){
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
