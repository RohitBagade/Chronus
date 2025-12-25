package com.chronos.chronos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    private static final String UPLOAD_DIR ="uploads/";

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            File dir = new File(System.getProperty("user.dir"), UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filePath = UPLOAD_DIR + file.getOriginalFilename();
            file.transferTo(new File(System.getProperty("user.dir"), filePath));

            // return only relative path (not full Windows path)
            return ResponseEntity.ok(filePath);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

}
