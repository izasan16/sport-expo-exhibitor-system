package com.sportexpo.exhibitor.sales;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.exhibitor.ExhibitorRepository;
import com.sportexpo.exhibitor.security.DataScopeService;
import com.sportexpo.exhibitor.users.User;
import com.sportexpo.exhibitor.users.UserRepository;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SalesAssignmentService {
    private static final String ASSIGNMENT_CONFLICT_CODE = "E_SALES_ASSIGNMENT_CONFLICT";

    private final ExhibitorSalesAssignmentRepository assignmentRepository;
    private final ExhibitorRepository exhibitorRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private final AuditService auditService;

    public SalesAssignmentService(ExhibitorSalesAssignmentRepository assignmentRepository,
                                  ExhibitorRepository exhibitorRepository,
                                  UserRepository userRepository,
                                  CacheManager cacheManager,
                                  AuditService auditService) {
        this.assignmentRepository = assignmentRepository;
        this.exhibitorRepository = exhibitorRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SalesAssignmentResponse getAssignments(UUID exhibitorId) {
        exhibitorRepository.findById(exhibitorId)
                .orElseThrow(() -> new BusinessException("Exhibitor does not exist"));
        return toSummary(exhibitorId, assignmentRepository.findByExhibitorId(exhibitorId));
    }

    @Transactional
    public SalesAssignmentResponse updateAssignments(UUID exhibitorId, SalesAssignmentRequest request, UUID actorId) {
        exhibitorRepository.findById(exhibitorId)
                .orElseThrow(() -> new BusinessException("Exhibitor does not exist"));
        var existing = assignmentRepository.findLockedByExhibitorId(exhibitorId);
        var oldSalesIds = existing.stream()
                .map(ExhibitorSalesAssignment::getSalesUserId)
                .collect(Collectors.toSet());

        var normalized = normalizeRequest(request);
        validateSalesUsers(normalized);

        assignmentRepository.deleteAll(existing);
        assignmentRepository.flush();

        var now = OffsetDateTime.now();
        var replacements = buildAssignments(exhibitorId, normalized, actorId, now);
        try {
            if (!replacements.isEmpty()) {
                assignmentRepository.saveAll(replacements);
                assignmentRepository.flush();
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("Cannot assign duplicate primary sales", ASSIGNMENT_CONFLICT_CODE);
        }

        var newSalesIds = replacements.stream()
                .map(ExhibitorSalesAssignment::getSalesUserId)
                .collect(Collectors.toSet());
        evictSalesScopeCaches(oldSalesIds, newSalesIds);
        auditService.record(null, actorId, "SALES_ASSIGNMENT_UPDATED", "Exhibitor",
                exhibitorId,
                existing.isEmpty() ? "UNASSIGNED" : "ASSIGNED",
                replacements.isEmpty() ? "UNASSIGNED" : "ASSIGNED",
                "sales assignment updated");
        return toSummary(exhibitorId, replacements);
    }

    private NormalizedSalesAssignmentRequest normalizeRequest(SalesAssignmentRequest request) {
        if (request == null) {
            throw new BusinessException("Sales assignment request must not be null");
        }
        var collaboratorIds = request.collaboratorSalesUserIds().stream()
                .filter(id -> id != null)
                .toList();
        var uniqueIds = new LinkedHashSet<UUID>();
        if (request.primarySalesUserId() != null && !uniqueIds.add(request.primarySalesUserId())) {
            throw new BusinessException("Cannot assign duplicate primary sales", ASSIGNMENT_CONFLICT_CODE);
        }
        for (var collaboratorId : collaboratorIds) {
            if (!uniqueIds.add(collaboratorId)) {
                throw new BusinessException("Cannot assign duplicate primary sales", ASSIGNMENT_CONFLICT_CODE);
            }
        }
        return new NormalizedSalesAssignmentRequest(request.primarySalesUserId(), collaboratorIds);
    }

    private void validateSalesUsers(NormalizedSalesAssignmentRequest request) {
        var userIds = request.allSalesUserIds();
        if (userIds.isEmpty()) {
            return;
        }
        var users = userRepository.findByIdIn(userIds);
        if (users.size() != userIds.size()) {
            throw new BusinessException("Assigned sales user does not exist");
        }
        for (var user : users) {
            if (user.isDisabled() || !user.roleCodes().contains("SALES")) {
                throw new BusinessException("Assigned sales user must be an active sales account");
            }
        }
    }

    private List<ExhibitorSalesAssignment> buildAssignments(UUID exhibitorId, NormalizedSalesAssignmentRequest request,
                                                            UUID actorId, OffsetDateTime now) {
        var assignments = new ArrayList<ExhibitorSalesAssignment>();
        if (request.primarySalesUserId() != null) {
            assignments.add(new ExhibitorSalesAssignment(
                    UUID.randomUUID(),
                    exhibitorId,
                    request.primarySalesUserId(),
                    ExhibitorSalesAssignment.TYPE_PRIMARY,
                    now,
                    now,
                    actorId,
                    actorId
            ));
        }
        for (var collaboratorSalesId : request.collaboratorSalesUserIds()) {
            assignments.add(new ExhibitorSalesAssignment(
                    UUID.randomUUID(),
                    exhibitorId,
                    collaboratorSalesId,
                    ExhibitorSalesAssignment.TYPE_COLLABORATOR,
                    now,
                    now,
                    actorId,
                    actorId
            ));
        }
        return assignments;
    }

    private void evictSalesScopeCaches(Set<UUID> oldSalesIds, Set<UUID> newSalesIds) {
        var cache = cacheManager.getCache(DataScopeService.SALES_ACCESSIBLE_EXHIBITOR_IDS_CACHE);
        if (cache == null) {
            return;
        }
        var affectedSalesIds = new LinkedHashSet<UUID>();
        affectedSalesIds.addAll(oldSalesIds);
        affectedSalesIds.addAll(newSalesIds);
        affectedSalesIds.forEach(cache::evict);
    }

    private SalesAssignmentResponse toSummary(UUID exhibitorId, Collection<ExhibitorSalesAssignment> assignments) {
        UUID primarySalesUserId = null;
        var collaboratorSalesUserIds = new ArrayList<UUID>();
        for (var assignment : assignments) {
            if (ExhibitorSalesAssignment.TYPE_PRIMARY.equals(assignment.getAssignmentType())) {
                primarySalesUserId = assignment.getSalesUserId();
            } else if (ExhibitorSalesAssignment.TYPE_COLLABORATOR.equals(assignment.getAssignmentType())) {
                collaboratorSalesUserIds.add(assignment.getSalesUserId());
            }
        }
        collaboratorSalesUserIds.sort(Comparator.comparing(UUID::toString));
        return new SalesAssignmentResponse(exhibitorId, primarySalesUserId, List.copyOf(collaboratorSalesUserIds));
    }

    private record NormalizedSalesAssignmentRequest(UUID primarySalesUserId, List<UUID> collaboratorSalesUserIds) {
        private Set<UUID> allSalesUserIds() {
            var ids = new LinkedHashSet<UUID>();
            if (primarySalesUserId != null) {
                ids.add(primarySalesUserId);
            }
            ids.addAll(collaboratorSalesUserIds);
            return ids;
        }
    }
}
