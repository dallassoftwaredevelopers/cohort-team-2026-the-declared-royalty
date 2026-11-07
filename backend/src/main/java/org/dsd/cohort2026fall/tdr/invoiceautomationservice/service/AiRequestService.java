package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.*;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRequestService {

    private final OpenAIClient client;
    private final InvoiceUploadService invoiceUploadService;

//    sends a request to the AI model to analyze the invoice (based on its UUID within the blob storage) and returns the extracted values as a string
    public String analyzeInvoice(UUID invoiceId) {
        ResponseCreateParams requestParams = getRequestParams(invoiceUploadService.loadFile(invoiceId));
        return client.responses().create(requestParams).output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(ResponseOutputText::text)
                .collect(Collectors.joining("\n"));
    }

//    https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses&lang=java
//    generates request parameters for the AI model based on the provided prompt and schema file paths
    private ResponseCreateParams getRequestParams(InvoiceUploadService.InvoiceFile invoiceFile) {
        String schemaFilePath = "/llm.resources/llm_request.json";
        JSONObject extractedValuesStructure = getJsonFromFile("/llm.resources/extracted_values_list.json");
        return ResponseCreateParams.builder()
                .model("gpt-6-astra")
                    .inputOfResponse(
                            List.of(
//                                    provides the prompt to the AI model for processing
                                    ResponseInputItem.ofEasyInputMessage(
                                            EasyInputMessage.builder()
                                                    .role(EasyInputMessage.Role.SYSTEM)
                                                    .content(getPrompt())
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
                                                                                    .filename(invoiceFile.filename())
                                                                                    .fileData(getInvoiceBase64DataUrl(invoiceFile)) // Ensure this starts with "data:application/pdf;base64,"
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
    private String getPrompt(){
        String filePath = "/llm.resources/llm_prompt.txt";
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
//    private String getInvoiceBase64DataUrl(String filename){
//        return "data:application/pdf;base64," + java.util.Base64.getEncoder().encodeToString(getInvoiceBase64(filename));
//    }
    private String getInvoiceBase64DataUrl(InvoiceUploadService.InvoiceFile invoiceFile){
        byte[] data = null;
        try{
            InputStream stream = invoiceFile.content();
            data = stream.readAllBytes();
            stream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "data:"+ getType(invoiceFile.filename()) + ";base64," + java.util.Base64.getEncoder().encodeToString(data);
    }

    private String getType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return invoiceUploadService.getContentTypes().getOrDefault(extension, "application/octet-stream");
    }

}
