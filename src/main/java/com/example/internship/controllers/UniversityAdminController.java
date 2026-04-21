package com.example.internship.controllers;

import com.example.internship.models.User;
import com.example.internship.models.*;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UniversityRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.internship.models.User;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/university-admin")
public class UniversityAdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private InternshipRepository internshipRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    // Басты бет: университет стажировкалары мен өтінімдерді көру
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        University university = user.getUniversity();
        List<Internship> myInternships = internshipRepository.findByUniversity(university);

        // Барлық стажировкалар бойынша өтінімдерді жинау
        List<Application> allApplications = myInternships.stream()
                .flatMap(i -> i.getApplications().stream())
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("university", university);
        model.addAttribute("internships", myInternships);
        model.addAttribute("allApplications", allApplications);

        return "university/dashboard";
    }

    // Жаңа стажировка қосу (Оқу материалдарымен бірге)
// Жаңа стажировка қосу (Оқу материалдарымен бірге)
// Жаңа стажировка қосу (Оқу материалдарымен бірге)
    @PostMapping("/add-internship")
    public String addInternship(@ModelAttribute Internship internship,
                                @RequestParam("studyMaterials") String studyMaterials,
                                Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пайдаланушы табылмады"));

        University university = user.getUniversity();

        if (university == null) {
            return "redirect:/university-admin/dashboard?error=no_university";
        }

        internship.setUniversity(university);
        // Модерацияға жіберу үшін статус КҮТУДЕ болуы шарт
        internship.setStatus(InternshipStatus.PENDING);
        internship.setStudyMaterials(studyMaterials);

        internshipRepository.save(internship);
        return "redirect:/university-admin/dashboard?message=sent_to_moderation";
    }

    // Өңдеу бетіне өту
    @GetMapping("/internship/edit/{id}")
    public String editInternship(@PathVariable Long id, Model model) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Стажировка не найдена"));

        model.addAttribute("internship", internship);
        return "university/edit-internship";
    }

    // Стажировка мәліметтерін жаңарту
    @PostMapping("/internship/update")
    public String updateInternship(@ModelAttribute("internship") Internship internship) {
        Internship existing = internshipRepository.findById(internship.getId())
                .orElseThrow(() -> new RuntimeException("Стажировка табылмады"));

        existing.setTitle(internship.getTitle());
        existing.setDescription(internship.getDescription());
        existing.setStudyMaterials(internship.getStudyMaterials());
        existing.setMaxPlaces(internship.getMaxPlaces());

        // Өңделгеннен кейін де статус қайтадан модерацияға кетеді
        existing.setStatus(InternshipStatus.PENDING);

        internshipRepository.save(existing);
        return "redirect:/university-admin/dashboard?success=updated_pending";
    }

    // Студенттің өтінімін растау
    @PostMapping("/application/verify/{id}")
    public String verifyApplication(@PathVariable Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Өтінім табылмады: " + id));

        app.setStatus(ApplicationStatus.VERIFIED);
        applicationRepository.save(app);

        return "redirect:/university-admin/dashboard?success=verified";
    }

    @PostMapping("/university/internships/save")
    public String saveInternship(@ModelAttribute("internship") Internship internship) {
        // internship.getStudyMaterials() ішінде формада жазылған мәтін келеді
        internshipRepository.save(internship);
        return "redirect:/university/internships";
    }
}