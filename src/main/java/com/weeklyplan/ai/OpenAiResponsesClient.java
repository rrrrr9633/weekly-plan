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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiResponsesClient {
  private static final long MAX_REQUEST_TIMEOUT_SECONDS = 180;
  private static final int STREAMING_THRESHOLD_BYTES = 12 * 1024;
  private static final Logger logger = LoggerFactory.getLogger(OpenAiResponsesClient.class);
  private final ObjectMapper mapper; private final String baseUrl; private final String apiKey; private final String model; private final String wireApi; private final Duration requestTimeout;
  public OpenAiResponsesClient(ObjectMapper mapper, @Value("${app.ai.base-url:}") String baseUrl, @Value("${app.ai.api-key:}") String apiKey, @Value("${app.ai.model:}") String model, @Value("${app.ai.wire-api:responses}") String wireApi, @Value("${app.ai.request-timeout-seconds:180}") long requestTimeoutSeconds) { this.mapper=mapper; this.baseUrl=baseUrl; this.apiKey=apiKey; this.model=model; this.wireApi=wireApi; this.requestTimeout=Duration.ofSeconds(Math.min(Math.max(requestTimeoutSeconds, 1), MAX_REQUEST_TIMEOUT_SECONDS)); }
  public AiDtos.ModelProposal propose(List<AiDtos.ChatMessage> history, String message, List<Map<String,Object>> writableProjects, List<Map<String,Object>> chatProjects, List<Map<String,Object>> chatPlans, int isoYear, int isoWeek) {
    if(baseUrl.isBlank()||apiKey.isBlank()||model.isBlank()||!"responses".equalsIgnoreCase(wireApi)) unavailable();
    try {
      String instructions = "Return exactly one JSON object with operationType, payload and preview; no markdown. Allowed operationType: PLAN_CREATE, PLAN_UPDATE, PLAN_DELETE, PROJECT_CREATE, PROJECT_UPDATE, PROJECT_DELETE, CHAT. Never return users, companies, roles, authentication or member operations or fields. Plan owner is always the current requester; do not emit owner/user fields. You are a helpful Chinese personal planning assistant. All requests that do not ask to create, update, or delete a plan or project must use CHAT. For CHAT, payload must be {} and preview must be a direct, useful Chinese answer based on the supplied read-only business context; never return a result count or ask the client to query data. If context is insufficient, say so plainly and ask one focused follow-up question. Do not invent plans, dates, project facts, or completion status. If the user asks to create/add/arrange a plan, always return PLAN_CREATE, even when content or project is missing; omit only the missing payload fields so the server can ask for them. Never turn an incomplete creation request into CHAT. Use a matching writable project's id when the request contains its available name or code. PLAN_CREATE payload schema is exactly {projectId:number, content:string, year?:number, weekNumber?:number, weekday?:\"MONDAY\"|\"TUESDAY\"|\"WEDNESDAY\"|\"THURSDAY\"|\"FRIDAY\"|\"SATURDAY\"|\"SUNDAY\"}. The server supplies year and weekNumber: when the user does not explicitly say a calendar date or relative week such as 下周, never ask for or infer the week; omit these fields. If a weekday is not explicit, omit weekday so the server asks only for 周几. Never use name, title, task or a numeric weekday for a plan. Current ISO year/week: " + isoYear + "/" + isoWeek + ". Writable current-company projects (ids, codes, names): " + mapper.writeValueAsString(writableProjects) + ". Read-only project context: " + mapper.writeValueAsString(chatProjects) + ". Read-only plan context: " + mapper.writeValueAsString(chatPlans) + ".";
      List<Map<String, Object>> input = new java.util.ArrayList<>();
      for (AiDtos.ChatMessage item : history) input.add(Map.of("role", item.role(), "content", item.content()));
      input.add(Map.of("role", "user", "content", message + "\n\nReturn the result as one JSON object."));
      Map<String, Object> payload = new LinkedHashMap<>(Map.of("model", model, "instructions", instructions, "input", input, "store", false, "text", Map.of("format", Map.of("type", "json_object"))));
      boolean streaming = mapper.writeValueAsBytes(payload).length > STREAMING_THRESHOLD_BYTES;
      if (streaming) payload.put("stream", true);
      String body = mapper.writeValueAsString(payload);
      HttpRequest request=HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/$","")+"/responses")).timeout(requestTimeout).header("Authorization","Bearer "+apiKey).header("Content-Type","application/json").header("Accept", streaming ? "text/event-stream" : "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> response = null;
      for (int attempt = 1; attempt <= 3; attempt++) {
        response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status >= 200 && status < 300) break;
        if ((status == 502 || status == 503 || status == 504) && attempt < 3) {
          logger.warn("AI 上游请求失败：HTTP {}，第 {}/3 次重试", status, attempt);
          Thread.sleep(attempt * 1000L);
          continue;
        }
        break;
      }
      if(response.statusCode()<200||response.statusCode()>=300) {
        String detail = response.body() == null ? "" : response.body().replaceAll("\\s+", " ");
        logger.warn("AI 上游请求失败：HTTP {}，地址={}，响应={}", response.statusCode(), baseUrl, detail.substring(0, Math.min(detail.length(), 500)));
        unavailable();
      }
      String text = streaming ? streamingText(response.body()) : responseText(response.body());
      if(text.isBlank()) throw new IllegalArgumentException("empty output");
      return mapper.treeToValue(mapper.readTree(text),AiDtos.ModelProposal.class);
    } catch(ResponseStatusException e){throw e;} catch(Exception e){
      logger.warn("AI 上游请求异常：{}，地址={}", e.getClass().getSimpleName(), baseUrl);
      unavailable(); return null;
    }
  }
  private String responseText(String body) throws Exception {
      JsonNode root=mapper.readTree(body);
      String text = root.path("output_text").asText();
      if (text.isBlank()) {
        for (JsonNode output : root.path("output")) {
          for (JsonNode content : output.path("content")) {
            if ("output_text".equals(content.path("type").asText())) {
              text = content.path("text").asText();
              if (!text.isBlank()) break;
            }
          }
          if (!text.isBlank()) break;
        }
      }
      return text;
  }
  private String streamingText(String body) throws Exception {
    StringBuilder deltas = new StringBuilder(); String completedText = "";
    for (String line : body.split("\\R")) {
      if (!line.startsWith("data:")) continue;
      String data = line.substring(5).trim();
      if (data.isBlank() || "[DONE]".equals(data)) continue;
      JsonNode event = mapper.readTree(data); String type = event.path("type").asText();
      if ("response.output_text.delta".equals(type)) deltas.append(event.path("delta").asText());
      else if ("response.output_text.done".equals(type)) completedText = event.path("text").asText();
    }
    return deltas.isEmpty() ? completedText : deltas.toString();
  }
  private void unavailable(){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI 服务暂不可用，请稍后重试");}
}
