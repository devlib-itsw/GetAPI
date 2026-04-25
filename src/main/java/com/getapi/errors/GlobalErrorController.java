package com.getapi.errors;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class GlobalErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public String handleErrorHtml(HttpServletRequest request, Model model) {
        int status = resolveStatus(request);
        model.addAttribute("status", status);
        return "error";
    }

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleErrorJson(HttpServletRequest request) {
        int status = resolveStatus(request);
        return ResponseEntity.status(status)
                .body(Map.of("status", status, "error", HttpStatus.resolve(status) != null
                        ? HttpStatus.resolve(status).getReasonPhrase() : "Error"));
    }

    private int resolveStatus(HttpServletRequest request) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusAttr instanceof Integer code) return code;
        return 500;
    }
}
