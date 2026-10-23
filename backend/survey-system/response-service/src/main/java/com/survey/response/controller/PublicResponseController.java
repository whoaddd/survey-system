package com.survey.response.controller;

import com.survey.common.ApiResponse;
import com.survey.response.dto.SubmitResponseRequest;
import com.survey.response.dto.SubmitResponseResult;
import com.survey.response.service.PublicResponseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/responses")
@RequiredArgsConstructor
public class PublicResponseController {

    private final PublicResponseService publicResponseService;

    @PostMapping
    public ApiResponse<SubmitResponseResult> submit(@RequestBody SubmitResponseRequest req,
                                                    HttpServletRequest request) {
        String ip = resolveClientIp(request);
        String ua = request.getHeader("User-Agent");
        return ApiResponse.ok(publicResponseService.submit(req, ip, ua));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}