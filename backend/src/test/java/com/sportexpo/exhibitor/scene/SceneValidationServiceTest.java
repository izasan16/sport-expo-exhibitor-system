package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.scene.dto.SceneValidationIssueResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SceneValidationServiceTest extends TestDatabaseSupport {
    @Autowired
    private SceneValidationService service;

    @Autowired
    private TestBusinessFixture fixture;

    @Test
    void returnsIssueWhenBoothFallsOutsideHallBoundary() {
        var draft = fixture.sceneDraftWithOutsideBooth();

        var result = service.validateDraft(draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.issues()).extracting(SceneValidationIssueResponse::code)
                .contains("BOOTH_OUTSIDE_HALL");
    }
}
