package org.dsd.cohort2026fall.tdr.invoiceautomationservice.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Value("${llm.api-key}")
    private String llmApiKey;

    @Bean
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(llmApiKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .build();
    }
}
