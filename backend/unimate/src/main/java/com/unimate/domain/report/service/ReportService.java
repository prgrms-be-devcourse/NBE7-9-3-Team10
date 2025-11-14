package com.unimate.domain.report.service;

import com.unimate.domain.report.dto.ReportCreateRequest;
import com.unimate.domain.user.user.entity.User;
import com.unimate.domain.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository   userRepository;

    @Transactional
    public ReportResponse create(String reporterEmail, ReportCreateRequest rq)
    {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(()-> new IllegalArgumentException("신고자 이메일 없음"));
        User reported = userRepository.findByEmail(rq.getReportedEmail())
                .orElseThrow(()-> new IllegalArgumentException("피신고자 이메일 없음"));

        //테스트용 자신 신고 불가
        if(reporter.getId().equals(reported.getId()))
            throw new IllegalArgumentException("자신은 신고 불가");

        Report saved = reportRepository.save(
                Report.builder()
                        .reporter(reporter)
                        .reported(reported)
                        .category(rq.getCategory())
                        .content(rq.getContent())
                        .reportStatus(ReportStatus.RECEIVED)
                        .build()
        );

        return ReportResponse.builder()
                .reportId(saved.getId())
                .reporterEmail(reporterEmail)
                .reportedEmail(rq.getReportedEmail())
                .category(saved.getCategory())
                .content(saved.getContent())
                .reportStatus(saved.getReportStatus())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

}
