package com.example.demo.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class AdminUploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge() {
        return "redirect:/admin/data?uploadTooLarge";
    }
}