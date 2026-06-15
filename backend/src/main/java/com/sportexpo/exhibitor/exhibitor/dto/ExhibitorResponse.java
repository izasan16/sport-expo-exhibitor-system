package com.sportexpo.exhibitor.exhibitor.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExhibitorResponse(UUID id, String companyName, String creditCode, String contactName,
                                String contactPhone, String contactEmail, String address, String status,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
