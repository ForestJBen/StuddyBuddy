package studdybuddy.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import studdybuddy.dto.SessionPlanRequest;
import studdybuddy.dto.SessionPlanResponse;
import studdybuddy.dto.SessionSummaryRequest;
import studdybuddy.dto.SessionSummaryResponse;

@Service
public class AiService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public String translateToEnglish(String text) {

        try {

            String apiKey = System.getenv("OPENAI_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                return "API key not found";
            }

            String requestBody = """
                    {
                      "model": "gpt-5.4-mini",
                      "input": "Translate the following text into clear natural English. Return only the English translation: %s"
                    }
                    """.formatted(escapeJson(text));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "OpenAI status code: " + response.statusCode()
            );

            if (response.statusCode() != 200) {
                return "OpenAI API error: " + response.body();
            }

            JsonNode root =
                     jsonMapper.readTree(response.body());

            JsonNode output =
                    root.path("output");

            for (JsonNode item : output) {

                JsonNode content =
                        item.path("content");

                for (JsonNode part : content) {

                    if ("output_text".equals(
                            part.path("type").asText())) {

                        return part.path("text").asText();
                    }
                }
            }

            return "No translation returned";

        } catch (Exception e) {

            return "Translation failed: "
                    + e.getMessage();
        }
    }

    public SessionPlanResponse generateSessionPlan(SessionPlanRequest request) {

        try {
            String apiKey = System.getenv("OPENAI_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                return null;
            }

            String prompt = """
                You are an AI study session facilitator.

                Create a study session plan using the following information:

                Course: %s
                Topic: %s
                Goal: %s
                Study style: %s
                Group size: %d
                Duration: %d minutes

                Return exactly in this format:

                ICEBREAKER:
                one icebreaker question

                DISCUSSION:
                question 1
                question 2
                question 3

                AGENDA:
                item 1
                item 2
                item 3
                item 4

                CHECKLIST:
                item 1
                item 2
                item 3
                """
                    .formatted(
                            request.getCourse(),
                            request.getTopic(),
                            request.getGoal(),
                            request.getStudyStyle(),
                            request.getGroupSize(),
                            request.getDuration()
                    );

            String requestBody = """
                {
                  "model": "gpt-5.4-mini",
                  "input": "%s"
                }
                """.formatted(escapeJson(prompt));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                System.out.println(response.body());
                return null;
            }

            JsonNode root = jsonMapper.readTree(response.body());

            String aiText = null;

            for (JsonNode item : root.path("output")) {

                for (JsonNode part : item.path("content")) {

                    if ("output_text".equals(part.path("type").asText())) {
                        aiText = part.path("text").asText();
                        break;
                    }
                }
            }

            if (aiText == null) {
                return null;
            }

            System.out.println(aiText);

            SessionPlanResponse result = new SessionPlanResponse();

            // 1. ICEBREAKER
            String[] icebreakerSplit = aiText.split("DISCUSSION:");

            String icebreakerPart = icebreakerSplit[0]
                    .replace("ICEBREAKER:", "")
                    .trim();

            // 2. DISCUSSION
            String[] discussionSplit =
                    icebreakerSplit[1].split("AGENDA:");

            String discussionPart =
                    discussionSplit[0].trim();

            // 3. AGENDA
            String[] agendaSplit =
                    discussionSplit[1].split("CHECKLIST:");

            String agendaPart =
                    agendaSplit[0].trim();

            // 4. CHECKLIST
            String checklistPart =
                    agendaSplit[1].trim();

            // Put parsed values into response
            result.setIcebreaker(icebreakerPart);

            result.setDiscussionPrompts(
                    java.util.Arrays.stream(
                                    discussionPart.split("\\R"))
                            .filter(line -> !line.isBlank())
                            .toList()
            );

            result.setAgenda(
                    java.util.Arrays.stream(
                                    agendaPart.split("\\R"))
                            .filter(line -> !line.isBlank())
                            .toList()
            );

            result.setChecklist(
                    java.util.Arrays.stream(
                                    checklistPart.split("\\R"))
                            .filter(line -> !line.isBlank())
                            .toList()
            );

            return result;

        } catch (Exception e) {

            System.out.println(
                    "Session plan generation failed: "
                            + e.getMessage()
            );

            return null;
        }
    }

    public SessionSummaryResponse generateSessionSummary(
            SessionSummaryRequest request) {

        try {
            String apiKey = System.getenv("OPENAI_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                return null;
            }

            String prompt = """
                You are an AI study session facilitator.

                Summarize the following study session notes:

                %s

                Return exactly in this format:

                COMPLETED:
                item 1
                item 2

                REMAINING:
                item 1

                FOLLOW_UP:
                item 1

                NEXT_GOAL:
                one concise next-session goal
                """
                    .formatted(request.getNotes());

            String requestBody = """
                {
                  "model": "gpt-5.4-mini",
                  "input": "%s"
                }
                """.formatted(escapeJson(prompt));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                System.out.println(response.body());
                return null;
            }

            JsonNode root =
                    jsonMapper.readTree(response.body());

            String aiText = null;

            for (JsonNode item : root.path("output")) {

                for (JsonNode part : item.path("content")) {

                    if ("output_text".equals(
                            part.path("type").asText())) {

                        aiText = part.path("text").asText();
                        break;
                    }
                }
            }

            if (aiText == null) {
                return null;
            }

            System.out.println(aiText);

            SessionSummaryResponse result =
                    new SessionSummaryResponse();

            String[] completedSplit =
                    aiText.split("REMAINING:");

            String completedPart =
                    completedSplit[0]
                            .replace("COMPLETED:", "")
                            .trim();

            String[] remainingSplit =
                    completedSplit[1].split("FOLLOW_UP:");

            String remainingPart =
                    remainingSplit[0].trim();

            String[] followUpSplit =
                    remainingSplit[1].split("NEXT_GOAL:");

            String followUpPart =
                    followUpSplit[0].trim();

            String nextGoalPart =
                    followUpSplit[1].trim();

            result.setTopicsCompleted(
                    java.util.Arrays.stream(
                                    completedPart.split("\\R"))
                            .filter(line -> !line.isBlank())
                            .toList()
            );

            result.setRemainingProblems(
                    java.util.Arrays.stream(
                                    remainingPart.split("\\R"))
                            .filter(line -> !line.isBlank())
                            .toList()
            );

            result.setFollowUpTasks(
                    java.util.Arrays.stream(
                                    followUpPart.split("\\R"))
                            .filter(line -> !line.isBlank())
                            .toList()
            );

            result.setNextSessionGoal(nextGoalPart);

            return result;

        } catch (Exception e) {

            System.out.println(
                    "Session summary generation failed: "
                            + e.getMessage()
            );

            return null;
        }
    }

    private String escapeJson(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}