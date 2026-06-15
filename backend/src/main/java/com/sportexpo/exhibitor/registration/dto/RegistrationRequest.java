package com.sportexpo.exhibitor.registration.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistrationRequest(UUID eventId, UUID exhibitorId, String category, UUID zoneId,
                                  BigDecimal intentAreaSqm, String contactName, String contactPhone, String remark) {
}
