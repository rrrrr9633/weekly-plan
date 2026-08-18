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
  public AiDtos.ModelProposal propose(String message, List<Map<String,Object>> writableProjects, List<Map<String,Object>> chatProjects, List<Map<String,Object>> chatPlans, int isoYear, int isoWeek) {
    if(baseUrl.isBlank()||apiKey.isBlank()||model.isBlank()||!"responses".equalsIgnoreCase(wireApi)) unavailable();
    try {
      String instructions = "Return exactly one JSON object with operationType, payload and preview; no markdown. Allowed operationType: PLAN_CREATE, PLAN_UPDATE, PLAN_DELETE, PROJECT_CREATE, PROJECT_UPDATE, PROJECT_DELETE, CHAT. Never return users, companies, roles, authentication or member operations or fields. Plan owner is always the current requester; do not emit owner/user fields. You are a helpful Chinese personal planning assistant. All requests that do not ask to create, update, or delete a plan or project must use CHAT. For CHAT, payload must be {} and preview must be a direct, useful Chinese answer based on the supplied read-only business context; never return a result count or ask the client to query data. If context is insufficient, say so plainly and ask one focused follow-up question. Do not invent plans, dates, project facts, or completion status. If the user asks to create/add/arrange a plan, always return PLAN_CREATE, even when content or project is missing; omit only the missing payload fields so the server can ask for them. Never turn an incomplete creation request into CHAT. Use a matching writable project's id when the request contains its available name or code. PLAN_CREATE payload schema is exactly {projectId:number, content:string, year?:number, weekNumber?:number, weekday?:\"MONDAY\"|\"TUESDAY\"|\"WEDNESDAY\"|\"THURSDAY\"|\"FRIDAY\"|\"SATURDAY\"|\"SUNDAY\"}. The server supplies year and weekNumber: when the user does not explicitly say a calendar date or relative week such as 下周, never ask for or infer the week; omit these fields. If a weekday is not explicit, omit weekday so the server asks only for 周几. Never use name, title, task or a numeric weekday for a plan. Current ISO year/week: " + isoYear + "/" + isoWeek + ". Writable current-company projects (ids, codes, names): " + mapper.writeValueAsString(writableProjects) + ". Read-only project context: " + mapper.writeValueAsString(chatProjects) + ". Read-only plan context: " + mapper.writeValueAsString(chatPlans) + ".";
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
