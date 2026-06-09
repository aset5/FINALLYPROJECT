package com.example.internship.services;

import com.example.internship.dto.ai.*;
import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.models.User;
import com.example.internship.repositories.InternshipRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentAiService {

    private final AiService aiService;
    private final InternshipRepository internshipRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentAiService(AiService aiService, InternshipRepository internshipRepository) {
        this.aiService = aiService;
        this.internshipRepository = internshipRepository;
    }

    public ImproveResumeResponse improveResume(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("Сначала заполните текст резюме");
        }

        String prompt = """
                Улучши резюме студента. Пиши на русском языке.
                Сохрани правдивость — не выдумывай опыт и навыки, только улучши формулировки и структуру.

                Исходный текст резюме:
                %s

                Ответь СТРОГО валидным JSON без markdown в формате:
                {"improvedText":"улучшенный текст резюме","tips":["совет 1","совет 2","совет 3"]}
                """.formatted(resumeText.trim());

        String raw = aiService.generateResponse(prompt);
        return parseImproveResume(raw, resumeText);
    }

    public JobMatchResponse analyzeJobMatches(User student) {
        String resume = student.getResume();
        if (resume == null || resume.isBlank()) {
            throw new IllegalArgumentException("Заполните текст резюме в профиле для AI-подбора");
        }

        List<Internship> jobs = internshipRepository.findAll().stream()
                .filter(i -> i.getCompany() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .toList();

        if (jobs.isEmpty()) {
            return new JobMatchResponse(
                    List.of(),
                    "Сейчас нет одобренных вакансий для анализа.",
                    new JobMatchStats(0, 0, 0, 0, null, 0)
            );
        }

        String jobsJson = buildJobsJson(jobs);
        String prompt = """
                Ты HR-аналитик платформы стажировок INTERN.PRO.
                Оцени соответствие резюме студента каждой вакансии.

                Резюме студента:
                %s

                Вакансии (JSON):
                %s

                Для каждой вакансии укажи matchPercent от 0 до 100, краткий summary (1 предложение) и 2-3 навыка для улучшения.
                Ответь СТРОГО валидным JSON без markdown:
                {
                  "overallAdvice": "общий совет студенту",
                  "matches": [
                    {
                      "internshipId": 1,
                      "matchPercent": 75,
                      "summary": "кратко почему подходит",
                      "skillsToImprove": ["навык1", "навык2"]
                    }
                  ]
                }
                Используй internshipId из списка вакансий. Включи все вакансии из списка.
                """.formatted(resume.trim(), jobsJson);

        String raw = aiService.generateResponse(prompt);
        return parseJobMatches(raw, jobs);
    }

    private String buildJobsJson(List<Internship> jobs) {
        try {
            List<Map<String, Object>> list = jobs.stream().map(j -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("internshipId", j.getId());
                m.put("title", j.getTitle());
                m.put("company", j.getCompany() != null ? j.getCompany().getName() : "");
                m.put("city", j.getCity() != null ? j.getCity() : "");
                m.put("description", truncate(j.getDescription(), 400));
                return m;
            }).toList();
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка подготовки вакансий", e);
        }
    }

    private ImproveResumeResponse parseImproveResume(String raw, String fallback) {
        try {
            JsonNode node = objectMapper.readTree(aiService.extractJson(raw));
            String improved = node.path("improvedText").asText(fallback);
            List<String> tips = new ArrayList<>();
            node.path("tips").forEach(t -> tips.add(t.asText()));
            if (tips.isEmpty()) {
                tips.addAll(List.of(
                        "Добавьте конкретные проекты",
                        "Укажите технологии и результат",
                        "Сократите общие фразы"));
            }
            return new ImproveResumeResponse(improved, tips);
        } catch (Exception e) {
            return new ImproveResumeResponse(
                    raw.length() > 50 ? raw : fallback,
                    List.of("Проверьте структуру: опыт → навыки → цели", "Добавьте цифры и достижения")
            );
        }
    }

    private JobMatchResponse parseJobMatches(String raw, List<Internship> jobs) {
        Map<Long, Internship> byId = jobs.stream().collect(Collectors.toMap(Internship::getId, j -> j));

        try {
            JsonNode root = objectMapper.readTree(aiService.extractJson(raw));
            String overallAdvice = root.path("overallAdvice").asText("Продолжайте развивать навыки из описаний вакансий.");
            List<JobMatchItem> items = new ArrayList<>();

            for (JsonNode m : root.path("matches")) {
                long id = m.path("internshipId").asLong(-1);
                Internship job = byId.get(id);
                if (job == null) continue;

                int percent = Math.min(100, Math.max(0, m.path("matchPercent").asInt(0)));
                List<String> skills = new ArrayList<>();
                m.path("skillsToImprove").forEach(s -> skills.add(s.asText()));

                items.add(new JobMatchItem(
                        job.getId(),
                        job.getTitle(),
                        job.getCompany() != null ? job.getCompany().getName() : "",
                        job.getCity(),
                        percent,
                        m.path("summary").asText(""),
                        skills
                ));
            }

            // Добавить вакансии, которые AI пропустил
            Set<Long> covered = items.stream().map(JobMatchItem::internshipId).collect(Collectors.toSet());
            for (Internship job : jobs) {
                if (!covered.contains(job.getId())) {
                    items.add(new JobMatchItem(
                            job.getId(),
                            job.getTitle(),
                            job.getCompany() != null ? job.getCompany().getName() : "",
                            job.getCity(),
                            50,
                            "Требуется ручная оценка",
                            List.of()
                    ));
                }
            }

            items.sort((a, b) -> Integer.compare(b.matchPercent(), a.matchPercent()));
            JobMatchStats stats = computeStats(jobs.size(), items);
            return new JobMatchResponse(items, overallAdvice, stats);
        } catch (Exception e) {
            List<JobMatchItem> fallback = jobs.stream()
                    .map(j -> new JobMatchItem(
                            j.getId(), j.getTitle(),
                            j.getCompany() != null ? j.getCompany().getName() : "",
                            j.getCity(), 0, "AI-анализ временно недоступен", List.of()))
                    .toList();
            return new JobMatchResponse(
                    fallback,
                    "Не удалось разобрать ответ AI. Попробуйте позже.",
                    computeStats(jobs.size(), fallback)
            );
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private JobMatchStats computeStats(int totalJobs, List<JobMatchItem> items) {
        if (items.isEmpty()) {
            return new JobMatchStats(totalJobs, 0, 0, 0, null, 0);
        }
        double avg = items.stream().mapToInt(JobMatchItem::matchPercent).average().orElse(0);
        int high = (int) items.stream().filter(i -> i.matchPercent() >= 70).count();
        JobMatchItem best = items.get(0);
        return new JobMatchStats(
                totalJobs,
                items.size(),
                Math.round(avg * 10) / 10.0,
                high,
                best.internshipId(),
                best.matchPercent()
        );
    }
}
