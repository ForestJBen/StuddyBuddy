package studdybuddy.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import studdybuddy.dto.SessionPlanRequest;
import studdybuddy.dto.SessionPlanResponse;
import studdybuddy.dto.SessionSummaryRequest;
import studdybuddy.dto.SessionSummaryResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AiService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public String translateToEnglish(String text) {

        try {
            String apiKey = System.getenv("OPENAI_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                throw new RuntimeException(
                        "AI service is currently unavailable"
                );
            }

            String requestBody = """
                    {
                      "model": "gpt-5.4-mini",
                      "input": "Translate the following text into clear natural English. Return only the English translation: %s"
                    }
                    """.formatted(escapeJson(text));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.openai.com/v1/responses"
                    ))
                    .header(
                            "Authorization",
                            "Bearer " + apiKey
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    requestBody
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "OpenAI status code: "
                            + response.statusCode()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "AI service is currently unavailable"
                );
            }

            JsonNode root =
                    jsonMapper.readTree(
                            response.body()
                    );

            JsonNode output =
                    root.path("output");

            for (JsonNode item : output) {

                JsonNode content =
                        item.path("content");

                for (JsonNode part : content) {

                    if ("output_text".equals(
                            part.path("type").asText())) {

                        return part
                                .path("text")
                                .asText();
                    }
                }
            }

            throw new RuntimeException(
                    "AI service is currently unavailable"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "AI service is currently unavailable"
            );
        }
    }

    public SessionPlanResponse generateSessionPlan(
            SessionPlanRequest request) {

        try {
            String apiKey =
                    System.getenv("OPENAI_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                throw new RuntimeException(
                        "AI service is currently unavailable"
                );
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
                    """.formatted(
                    escapeJson(prompt)
            );

            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            "https://api.openai.com/v1/responses"
                                    )
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            requestBody
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {

                System.out.println(
                        response.body()
                );

                throw new RuntimeException(
                        "AI service is currently unavailable"
                );
            }

            JsonNode root =
                    jsonMapper.readTree(
                            response.body()
                    );

            String aiText = null;

            for (JsonNode item :
                    root.path("output")) {

                for (JsonNode part :
                        item.path("content")) {

                    if ("output_text".equals(
                            part.path("type").asText())) {

                        aiText =
                                part.path("text").asText();

                        break;
                    }
                }
            }

            if (aiText == null) {
                throw new RuntimeException(
                        "AI service is currently unavailable"
                );
            }

            System.out.println(aiText);

            SessionPlanResponse result =
                    new SessionPlanResponse();

            String[] icebreakerSplit =
                    aiText.split("DISCUSSION:");

            String icebreakerPart =
                    icebreakerSplit[0]
                            .replace(
                                    "ICEBREAKER:",
                                    ""
                            )
                            .trim();

            String[] discussionSplit =
                    icebreakerSplit[1]
                            .split("AGENDA:");

            String discussionPart =
                    discussionSplit[0]
                            .trim();

            String[] agendaSplit =
                    discussionSplit[1]
                            .split("CHECKLIST:");

            String agendaPart =
                    agendaSplit[0]
                            .trim();

            String checklistPart =
                    agendaSplit[1]
                            .trim();

            result.setIcebreaker(
                    icebreakerPart
            );

            result.setDiscussionPrompts(
                    java.util.Arrays
                            .stream(
                                    discussionPart
                                            .split("\\R")
                            )
                            .filter(
                                    line ->
                                            !line.isBlank()
                            )
                            .toList()
            );

            result.setAgenda(
                    java.util.Arrays
                            .stream(
                                    agendaPart
                                            .split("\\R")
                            )
                            .filter(
                                    line ->
                                            !line.isBlank()
                            )
                            .toList()
            );

            result.setChecklist(
                    java.util.Arrays
                            .stream(
                                    checklistPart
                                            .split("\\R")
                            )
                            .filter(
                                    line ->
                                            !line.isBlank()
                            )
                            .toList()
            );

            return result;

        } catch (Exception e) {

            System.out.println(
                    "Session plan generation failed: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "AI service is currently unavailable"
            );
        }
    }

    public SessionSummaryResponse generateSessionSummary(
            SessionSummaryRequest request) {

        try {

            String apiKey = System.getenv("OPENAI_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                throw new RuntimeException(
                        "AI service is currently unavailable"
                );
            }

            String prompt = """
                You are an AI study session facilitator.

                The following information describes one study session.

                Course:
                %s

                Topic:
                %s

                Original study goal:
                %s

                Planned agenda:
                %s

                Planned checklist:
                %s

                Notes describing what actually happened:
                %s

                The agenda and checklist describe what was planned.
                The notes describe what actually happened.

                Use the notes as the primary source of truth.

                Compare the notes with the original plan and identify:
                - what was completed
                - important discussion points
                - what was planned but not completed
                - remaining problems or areas of confusion
                - follow-up tasks
                - one useful goal for the next study session

                Rules:
                - Only claim something was completed if the notes support it.
                - Do not invent completed work.
                - Do not invent follow-up tasks.
                - Preserve people's names when assigning tasks.
                - If nothing was planned but not completed, return an empty array.
                - If no remaining problem was mentioned, return an empty array.
                - If no follow-up task was mentioned, return an empty array.
                - Keep the session overview concise.

                Return ONLY valid JSON.
                Do not use Markdown.
                Do not use ```json.
                Do not add any explanation before or after the JSON.

                Use exactly this JSON structure:

                {
                  "sessionOverview": "short summary",
                  "topicsCompleted": [
                    "item"
                  ],
                  "keyDiscussionPoints": [
                    "item"
                  ],
                  "plannedButNotCompleted": [
                    "item"
                  ],
                  "remainingProblems": [
                    "item"
                  ],
                  "followUpTasks": [
                    "item"
                  ],
                  "nextSessionGoal": "one concise goal"
                }
                """
                    .formatted(
                            request.getCourse(),
                            request.getTopic(),
                            request.getGoal(),
                            request.getAgenda(),
                            request.getChecklist(),
                            request.getNotes()
                    );

            String requestBody = """
                {
                  "model": "gpt-5.4-mini",
                  "input": "%s"
                }
                """.formatted(escapeJson(prompt));

            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            "https://api.openai.com/v1/responses"
                                    )
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(requestBody)
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {

                System.out.println(response.body());

                throw new RuntimeException(
                        "AI service is currently unavailable"
                );
            }

            JsonNode root =
                    jsonMapper.readTree(response.body());

            String aiText = null;

            for (JsonNode item : root.path("output")) {

                for (JsonNode part : item.path("content")) {

                    if ("output_text".equals(
                            part.path("type").asText())) {

                        aiText =
                                part.path("text").asText();

                        break;
                    }
                }
            }

            if (aiText == null || aiText.isBlank()) {
                throw new RuntimeException(
                        "AI service is currently unavailable"
                );
            }

            System.out.println("AI summary JSON:");
            System.out.println(aiText);

            JsonNode summaryJson =
                    jsonMapper.readTree(aiText);

            SessionSummaryResponse result =
                    new SessionSummaryResponse();

            result.setSessionOverview(
                    summaryJson
                            .path("sessionOverview")
                            .asText()
            );

            result.setTopicsCompleted(
                    jsonArrayToList(
                            summaryJson.path("topicsCompleted")
                    )
            );

            result.setKeyDiscussionPoints(
                    jsonArrayToList(
                            summaryJson.path("keyDiscussionPoints")
                    )
            );

            result.setPlannedButNotCompleted(
                    jsonArrayToList(
                            summaryJson.path("plannedButNotCompleted")
                    )
            );

            result.setRemainingProblems(
                    jsonArrayToList(
                            summaryJson.path("remainingProblems")
                    )
            );

            result.setFollowUpTasks(
                    jsonArrayToList(
                            summaryJson.path("followUpTasks")
                    )
            );

            result.setNextSessionGoal(
                    summaryJson
                            .path("nextSessionGoal")
                            .asText()
            );

            return result;

        } catch (Exception e) {

            System.out.println(
                    "Session summary generation failed: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "AI service is currently unavailable"
            );
        }
    }

    private java.util.List<String> jsonArrayToList(
            JsonNode arrayNode) {

        java.util.List<String> result =
                new java.util.ArrayList<>();

        for (JsonNode item : arrayNode) {
            result.add(item.asText());
        }

        return result;
    }

    private String escapeJson(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}