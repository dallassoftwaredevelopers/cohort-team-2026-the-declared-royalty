package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

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

@Service
public class AiRequestService {

//    https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses&lang=java
//    generates request parameters for the AI model based on the provided prompt and schema file paths
    public ResponseCreateParams getRequestParams(String promptFilePath, String invoiceName, String schemaFilePath) {
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
                                                    .content(getInvoiceBase64DataUrl(invoiceName))
                                                    .build())))
//                provides the expected response format to the AI model, including the JSON schema for validation
                    .text(
                            ResponseTextConfig.builder()
                                    .format(
                                            ResponseFormatTextJsonSchemaConfig.builder()
                                                    .name("invoiceValues")
                                                    .strict(true)
                                                    .schema(
                                                            JsonValue.from(getSchema(schemaFilePath))
                                                                    .convert(ResponseFormatTextJsonSchemaConfig.Schema.class))
                                                    .build())
                                    .build())
                    .build();
    }

//    generates a JSON schema for the LLM response based on the sample JSON provided at the file path
    private Map<String, Object> getSchema(String filePath) {
        Map<String, Object> properties = getSchemaFromJsonObject(getJsonFromFile(filePath));
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", properties.keySet(),
                "additionalProperties", false
        );
    }

//    recursively generates a JSON schema for the LLM response based on the sample JSON Object provided
    private Map<String, Object> getSchemaFromJsonObject(JSONObject jsonObject){
        Map<String, Object> properties = new HashMap<>();
        for (String field : jsonObject.keySet()) {
            Object value = jsonObject.get(field);
            String type = determineJsonType(value);
            if(type.equals("object") && value instanceof JSONObject nestedObject) {
                properties.put(field, Map.of(
                        "type", "object",
                        "properties", getSchemaFromJsonObject(nestedObject)
                ));
            } else {
                properties.put(field, Map.of("type", type));
            }
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
