package com.kahua.assets.repository;

import com.kahua.assets.entity.Operation;
import com.kahua.assets.entity.TicketBook;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetTicketBookSpecificationTest {

    @Mock
    private Root<TicketBook> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<String> scanCodePath;

    @Mock
    private Path<Integer> startNumPath;

    @Mock
    private Path<Integer> endNumPath;

    @Mock
    private Path<UUID> ticketBookIdPath;

    @Mock
    private Expression<String> lowerScanCodeExpr;

    @Mock
    private Predicate equalScanCodePredicate;

    @Mock
    private Predicate likeScanCodePredicate;

    @Mock
    private Predicate equalStartNumPredicate;

    @Mock
    private Predicate equalEndNumPredicate;

    @Mock
    private Predicate equalTicketBookIdPredicate;

    @Mock
    private Predicate orCombinedPredicate;

    @Mock
    private Predicate andCombinedPredicate;

    @Mock
    private Join<TicketBook, Operation> operationJoin;

    @Mock
    private Path<UUID> operationIdPath;

    @Mock
    private Predicate operationEqualPredicate;

    @BeforeEach
    void setUp() {
        lenient().when(root.<String>get("scanCode")).thenReturn(scanCodePath);
        lenient().when(root.<Integer>get("startNum")).thenReturn(startNumPath);
        lenient().when(root.<Integer>get("endNum")).thenReturn(endNumPath);
        lenient().when(root.<UUID>get("ticketBookId")).thenReturn(ticketBookIdPath);
        lenient().when(cb.lower(eq(scanCodePath))).thenReturn(lowerScanCodeExpr);

        lenient().when(cb.equal(eq(lowerScanCodeExpr), anyString())).thenReturn(equalScanCodePredicate);
        lenient().when(cb.like(eq(lowerScanCodeExpr), anyString())).thenReturn(likeScanCodePredicate);
        lenient().when(cb.equal(eq(startNumPath), any(Integer.class))).thenReturn(equalStartNumPredicate);
        lenient().when(cb.equal(eq(endNumPath), any(Integer.class))).thenReturn(equalEndNumPredicate);
        lenient().when(cb.equal(eq(ticketBookIdPath), any(UUID.class))).thenReturn(equalTicketBookIdPredicate);

        lenient().when(cb.or(any(Predicate[].class))).thenReturn(orCombinedPredicate);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(andCombinedPredicate);
    }

    @Test
    @DisplayName("Integer search matches exact startNum and endNum, but NOT inside range")
    void searchForAsset_withInteger_matchesStartAndEndNumOnly() {
        Specification<TicketBook> spec = AssetTicketBookSpecification.searchForAsset(null, "500");
        spec.toPredicate(root, query, cb);

        // Verify exact startNum and endNum checks are present
        verify(cb).equal(eq(startNumPath), eq(500));
        verify(cb).equal(eq(endNumPath), eq(500));

        // Verify range checks (lessThanOrEqualTo, greaterThanOrEqualTo) are NOT present
        verify(cb, never()).lessThanOrEqualTo(any(), any(Comparable.class));
        verify(cb, never()).greaterThanOrEqualTo(any(), any(Comparable.class));

        // Scan code matches remain intact
        verify(cb).equal(eq(lowerScanCodeExpr), eq("500"));
        verify(cb).like(eq(lowerScanCodeExpr), eq("%500%"));
    }

    @Test
    @DisplayName("Non-integer search does not query startNum or endNum")
    void searchForAsset_withNonInteger_ignoresNumericPredicates() {
        Specification<TicketBook> spec = AssetTicketBookSpecification.searchForAsset(null, "SCAN-999");
        spec.toPredicate(root, query, cb);

        verify(cb, never()).equal(eq(startNumPath), any());
        verify(cb, never()).equal(eq(endNumPath), any());
        verify(cb, never()).lessThanOrEqualTo(any(), any(Comparable.class));
        verify(cb, never()).greaterThanOrEqualTo(any(), any(Comparable.class));

        verify(cb).equal(eq(lowerScanCodeExpr), eq("scan-999"));
        verify(cb).like(eq(lowerScanCodeExpr), eq("%scan-999%"));
    }

    @Test
    @DisplayName("UUID search matches ticketBookId and scan code")
    void searchForAsset_withUuid_matchesTicketBookId() {
        UUID randomId = UUID.randomUUID();
        Specification<TicketBook> spec = AssetTicketBookSpecification.searchForAsset(null, randomId.toString());
        spec.toPredicate(root, query, cb);

        verify(cb).equal(eq(ticketBookIdPath), eq(randomId));
        verify(cb, never()).equal(eq(startNumPath), any());
        verify(cb, never()).equal(eq(endNumPath), any());
        verify(cb).equal(eq(lowerScanCodeExpr), eq(randomId.toString().toLowerCase()));
    }

    @Test
    @DisplayName("OperationId filtering is applied when provided")
    void searchForAsset_withOperationId_appliesOperationPredicate() {
        UUID opId = UUID.randomUUID();
        when(root.<TicketBook, Operation>join("operation")).thenReturn(operationJoin);
        when(operationJoin.<UUID>get("operationId")).thenReturn(operationIdPath);
        when(cb.equal(eq(operationIdPath), eq(opId))).thenReturn(operationEqualPredicate);

        Specification<TicketBook> spec = AssetTicketBookSpecification.searchForAsset(opId, "100");
        spec.toPredicate(root, query, cb);

        verify(cb).equal(eq(operationIdPath), eq(opId));
        verify(cb).equal(eq(startNumPath), eq(100));
        verify(cb).equal(eq(endNumPath), eq(100));
    }

    @Test
    @DisplayName("Empty or null search term produces no search predicates")
    void searchForAsset_withEmptySearch_noSearchPredicates() {
        Specification<TicketBook> spec = AssetTicketBookSpecification.searchForAsset(null, "   ");
        spec.toPredicate(root, query, cb);

        verify(cb, never()).or(any(Predicate[].class));
        verify(cb, never()).equal(eq(startNumPath), any());
        verify(cb, never()).equal(eq(endNumPath), any());
    }
}
