package com.testhub.testflowlite.apitoken;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiTokenCreatedDto {
    private Long id;
    /**
     * Plaintext token string shown ONLY once upon generation.
     * Never stored in database, never retrievable again.
     */
    private String plainTextToken;
    private LocalDateTime createdAt;
}
