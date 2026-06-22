package com.sportexpo.exhibitor.sales;

import java.util.List;
import java.util.UUID;

public record SalesAssignmentResponse(UUID exhibitorId, UUID primarySalesUserId,
                                      List<UUID> collaboratorSalesUserIds) {
}
