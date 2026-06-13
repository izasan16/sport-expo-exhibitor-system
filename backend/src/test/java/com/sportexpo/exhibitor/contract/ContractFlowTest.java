package com.sportexpo.exhibitor.contract;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ContractFlowTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired ParticipationOrderService orderService;
    @Autowired ContractService contractService;
    @Autowired ContractRepository contractRepository;
    @Autowired ContractVersionRepository contractVersionRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Value("${app.db.schema}") String schema;

    @Test
    void salesConfirmGeneratesContractAndMarkEffectiveMovesOrderToPayment() {
        var order = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");

        var confirmed = orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(confirmed.status()).isEqualTo(BusinessStatuses.ORDER_PENDING_CONTRACT);
        var contract = contractRepository.findByOrderId(order.id()).orElseThrow();
        assertThat(contract.getStatus()).isEqualTo(BusinessStatuses.CONTRACT_GENERATED);
        var versions = contractVersionRepository.findByContractIdOrderByVersionNoAsc(contract.getId());
        assertThat(versions).hasSize(1);
        assertThat(contract.getCurrentVersionId()).isEqualTo(versions.get(0).getId());
        assertThat(versions.get(0).getSignStatus()).isEqualTo(BusinessStatuses.CONTRACT_VERSION_UNSIGNED);

        var effective = contractService.markOfflineSigned(contract.getId(), DemoActors.DEMO_ADMIN_ID);

        assertThat(effective.status()).isEqualTo(BusinessStatuses.CONTRACT_EFFECTIVE);
        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PENDING_PAYMENT);
        assertThat(contractVersionRepository.findByContractIdOrderByVersionNoAsc(contract.getId()).get(0).getSignStatus())
                .isEqualTo(BusinessStatuses.CONTRACT_VERSION_MARKED_SIGNED);
    }

    @Test
    void salesConfirmCannotCreateDuplicateContractsForSameOrder() {
        var order = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");

        orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);

        assertThatThrownBy(() -> orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order is not pending sales confirmation");
        var contracts = contractRepository.findByOrderIdOrderByCreatedAtDesc(order.id());
        assertThat(contracts).hasSize(1);
        assertThat(contractVersionRepository.findByContractIdOrderByVersionNoAsc(contracts.get(0).getId()))
                .hasSize(1);
    }

    @Test
    void salesConfirmStoresContractVersionSnapshotAsJsonObject() {
        var order = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");

        orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);

        var contract = contractRepository.findByOrderId(order.id()).orElseThrow();
        var version = contractVersionRepository.findByContractIdOrderByVersionNoAsc(contract.getId()).get(0);
        assertThat(contract.getCurrentVersionId()).isEqualTo(version.getId());
        assertThat(version.getContentSnapshot()).isInstanceOf(Map.class);
        assertThat(version.getContentSnapshot())
                .containsEntry("version", 1)
                .containsEntry("reason", "sales_confirm")
                .containsEntry("source", "offline_contract")
                .containsEntry("signMode", "offline_mark");
        var snapshotType = jdbcTemplate.queryForObject("""
                        select jsonb_typeof(content_snapshot)
                        from %s.contract_versions
                        where id = ?
                        """.formatted(schema),
                String.class,
                version.getId());
        assertThat(snapshotType).isEqualTo("object");
    }
}
