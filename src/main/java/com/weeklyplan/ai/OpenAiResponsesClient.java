package com.weeklyplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiResponsesClient {
  private static final Logger logger = LoggerFactory.getLogger(OpenAiResponsesClient.class);
  private final ObjectMapper mapper; private final String baseUrl; private final String apiKey; private final String model; private final String wireApi;
  public OpenAiResponsesClient(ObjectMapper mapper, @Value("${app.ai.base-url:}") String baseUrl, @Value("${app.ai.api-key:}") String apiKey, @Value("${app.ai.model:}") String model, @Value("${app.ai.wire-api:responses}") String wireApi) { this.mapper=mapper; this.baseUrl=baseUrl; this.apiKey=apiKey; this.model=model; this.wireApi=wireApi; }
  public AiDtos.ModelProposal propose(String message, List<Map<String,Object>> projects, int isoYear, int isoWeek) {
    if(baseUrl.isBlank()||apiKey.isBlank()||model.isBlank()||!"responses".equalsIgnoreCase(wireApi)) unavailable();
    try {
      String instructions = "Return exactly one JSON object with operationType, payload and preview; no markdown. Allowed operationType: PLAN_CREATE, PLAN_UPDATE, PLAN_DELETE, PROJECT_CREATE, PROJECT_UPDATE, PROJECT_DELETE, QUERY. Never return users, companies, roles, authentication or member operations or fields. Plan owner is always the current requester; do not emit owner/user fields. For PLAN_CREATE use projectId and current ISO year/week when omitted. Current ISO year/week: " + isoYear + "/" + isoWeek + ". Available current-company projects (only ids, codes and names): " + mapper.writeValueAsString(projects) + ". QUERY payload resource must be plans or projects.";
      String body=mapper.writeValueAsString(Map.of("model",model,"instructions",instructions,"input",message,"text",Map.of("format",Map.of("type","json_object"))));
      HttpRequest request=HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/$","")+"/responses")).timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+apiKey).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
      if(response.statusCode()<200||response.statusCode()>=300) {
        logger.warn("AI 上游请求失败：HTTP {}，地址={}", response.statusCode(), baseUrl);
        unavailable();
      }
      JsonNode root=mapper.readTree(response.body()); String text=root.path("output_text").asText(); if(text.isBlank()) text=root.path("output").path(0).path("content").path(0).path("text").asText(); if(text.isBlank()) throw new IllegalArgumentException("empty output");
      return mapper.treeToValue(mapper.readTree(text),AiDtos.ModelProposal.class);
    } catch(ResponseStatusException e){throw e;} catch(Exception e){
      logger.warn("AI 上游请求异常：{}，地址={}", e.getClass().getSimpleName(), baseUrl);
      unavailable(); return null;
    }
  }
  private void unavailable(){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI 服务暂不可用，请稍后重试");}
}
