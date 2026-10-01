package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class StatementBatchForm {
    private List<StatementUpload> uploads = new ArrayList<>();

    @Getter @Setter
    public static class StatementUpload {
        private Integer year;
        private Integer month;
        private MultipartFile file;
    }
}