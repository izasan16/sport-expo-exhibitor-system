package com.sportexpo.exhibitor.sales;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SalesAssignmentRequest(UUID primarySalesUserId, List<UUID> collaboratorSalesUserIds) {
    public SalesAssignmentRequest {
        collaboratorSalesUserIds = collaboratorSalesUserIds == null ? List.of() : List.copyOf(collaboratorSalesUserIds);
    }

    public Set<UUID> allSalesUserIds() {
        var ids = new LinkedHashSet<UUID>();
        if (primarySalesUserId != null) {
            ids.add(primarySalesUserId);
        }
        ids.addAll(collaboratorSalesUserIds);
        return ids;
    }
}
