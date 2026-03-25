package com.example.internship.controllers;

import com.example.internship.services.AiService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        String portalKnowledge =
                "Сен INTERN.PRO порталының ресми көмекшісісің. Төмендегі ережелерге сүйеніп қана жауап бер:\n" +
                        "1. Тіркелу: Студенттер мен компаниялар ЭЦП (keys.p12) арқылы немесе eGov Mobile арқылы тіркеле алады.\n" +
                        "2. Стажировка: Стажировка мерзімі әдетте 1 айдан 6 айға дейін созылады.\n" +
                        "3. Құжаттар: Шарттар мен келісімшарттар автоматты түрде жасалады және оларға онлайн қол қойылады.\n" +
                        "4. HR менеджерлер: Олар вакансия жариялап, студенттердің резюмесін (CV) тексере алады.\n" +
                        "5. Юридикалық талаптар: Барлық стажировкалар ҚР Еңбек кодексіне сәйкес рәсімделеді.\n" +
                        "Егер сұрақ осы тақырыптардан тыс болса, сыпайы түрде тек портал жөнінде көмектесе алатыныңды айт.\n" +
                        "Сұрақ: ";

        return aiService.generateResponse(portalKnowledge + message);
    }

    @PostMapping("/match")
    public String matchSkills(@RequestBody Map<String, String> data) {
        String studentSkills = data.get("skills");
        String jobRequirements = data.get("requirements");

        String prompt = String.format(
                "Рөл: HR-аналитик. Студенттің дағдылары: [%s]. Вакансия талаптары: [%s]. " +
                        "Сәйкестікті пайызбен (%%) есепте және студентке қандай 3 дағдыны жетілдіру керектігін қазақша тізіммен жазып бер.",
                studentSkills, jobRequirements
        );

        return aiService.generateResponse(prompt);
    }
}