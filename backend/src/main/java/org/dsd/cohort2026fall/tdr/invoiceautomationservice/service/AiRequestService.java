package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRequestService {

    private final OpenAIClient client;
    private final InvoiceUploadService invoiceUploadService;
    private final OpenApiToMapService openApiToMapService;

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
//    generates request parameters for the AI model based on the internal prompt and openAPI schema
    private ResponseCreateParams getRequestParams(InvoiceUploadService.InvoiceFile invoiceFile) {
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
//                                                          Schema structure is defined in openAPI documentation at "src/main/resources/openapi/components/schemas/OpenAiResponse.yaml"
                                                            Objects.requireNonNull(JsonValue.from(openApiToMapService.convertOpenApiToMap())
                                                                    .convert(ResponseFormatTextJsonSchemaConfig.Schema.class)))
                                                    .build())
                                    .build())
                    .build();
    }

//    reads the prompt located in the resources folder and returns it as a string
    private String getPrompt(){
        String filePath = "/llm.resources/llm_prompt.txt";
        try (var reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(filePath)),
                StandardCharsets.UTF_8)) {
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

//    converts the invoice file into a base64-encoded data URL for sending to the AI model
    private String getInvoiceBase64DataUrl(InvoiceUploadService.InvoiceFile invoiceFile){
        byte[] data;
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
