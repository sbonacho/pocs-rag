package com.severance.ai_rag.controller;

import java.util.HashMap;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/docs")
@Slf4j
public class DocController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:prompts/severance.ai.st")
    private Resource stPromptTemplate;

    public DocController(ChatClient.Builder chatBuilder, VectorStore vectorStore) {
        this.chatClient = chatBuilder.defaultAdvisors(new PromptChatMemoryAdvisor(new InMemoryChatMemory()))
            .build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/chat")
    public String generateResponse(@RequestParam String query) {
        PromptTemplate promptTemplate = new PromptTemplate(stPromptTemplate);
        var promptParameters = new HashMap<String, Object>();
        promptParameters.put("input", query);
        String context = String.join("\n", this.findSimilarDocuments(query));
        int tokenCount = context.split("\\s+").length;
        log.info(context);
        log.info("Found {} tokens in similar documents", tokenCount);
        promptParameters.put("documents", context);
        
                var prompt = promptTemplate.create(promptParameters);
                var response = this.chatClient.prompt(prompt).call().chatResponse();
        
                return response.getResult().getOutput().getContent();
    }
        
    private List<String> findSimilarDocuments(String query) {
        List<Document> similarDocuments = this.vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(3)
                .build()
            );
        return similarDocuments.stream().map(Document::getContent).toList();
    }    

}
