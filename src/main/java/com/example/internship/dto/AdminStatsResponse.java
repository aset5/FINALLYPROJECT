package com.example.internship.dto;

public record AdminStatsResponse(
        long totalUsers,
        long students,
        long companies,
        long universityAdmins,
        long pendingAccountApprovals,
        long pendingInternships,
        long approvedInternships,
        long totalApplications,
        long completedPrograms,
        long verifiedPrograms
) {
}
