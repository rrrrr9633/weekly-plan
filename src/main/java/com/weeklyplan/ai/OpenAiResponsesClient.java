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
      String instructions = "Return exactly one JSON object with operationType, payload and preview; no markdown. Allowed operationType: PLAN_CREATE, PLAN_UPDATE, PLAN_DELETE, PROJECT_CREATE, PROJECT_UPDATE, PROJECT_DELETE, QUERY. Never return users, companies, roles, authentication or member operations or fields. Plan owner is always the current requester; do not emit owner/user fields. This is a plan-operation parser, not a general chat assistant. If the user asks to create/add/arrange a plan, always return PLAN_CREATE, even when content or project is missing; omit only the missing payload fields so the server can ask for them. Never turn an incomplete creation request into QUERY. Do not return QUERY merely to ask which project to use when the request contains an available project name or code. Use the matching project's id. Only use QUERY when the user explicitly asks to view/list/search plans or projects. PLAN_CREATE payload schema is exactly {projectId:number, content:string, year?:number, weekNumber?:number, weekday?:\"MONDAY\"|\"TUESDAY\"|\"WEDNESDAY\"|\"THURSDAY\"|\"FRIDAY\"|\"SATURDAY\"|\"SUNDAY\"|\"PENDING\"}. Never use name, title, task or a numeric weekday for a plan. Omit year, weekNumber and weekday when the user did not explicitly provide a date; the server will default them. Current ISO year/week: " + isoYear + "/" + isoWeek + ". Available current-company projects (only ids, codes and names): " + mapper.writeValueAsString(projects) + ". QUERY payload resource must be plans or projects.";
      // Some OpenAI-compatible gateways validate the user input itself (not only
      // `instructions`) before allowing json_object response formatting.
      String input = "User request:\n" + message + "\n\nReturn the result as one JSON object.";
      String body=mapper.writeValueAsString(Map.of("model",model,"instructions",instructions,"input",input,"store",false,"text",Map.of("format",Map.of("type","json_object"))));
      HttpRequest request=HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/$","")+"/responses")).timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+apiKey).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
      if(response.statusCode()<200||response.statusCode()>=300) {
        logger.warn("AI 上游请求失败：HTTP {}，地址={}", response.statusCode(), baseUrl);
        unavailable();
      }
      JsonNode root=mapper.readTree(response.body());
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
      if(text.isBlank()) throw new IllegalArgumentException("empty output");
      return mapper.treeToValue(mapper.readTree(text),AiDtos.ModelProposal.class);
    } catch(ResponseStatusException e){throw e;} catch(Exception e){
      logger.warn("AI 上游请求异常：{}，地址={}", e.getClass().getSimpleName(), baseUrl);
      unavailable(); return null;
    }
  }
  private void unavailable(){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI 服务暂不可用，请稍后重试");}
}
