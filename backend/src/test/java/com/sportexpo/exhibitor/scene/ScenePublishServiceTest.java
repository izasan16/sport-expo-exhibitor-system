package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.booth.BoothRepository;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.scene.dto.PublishSceneRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ScenePublishServiceTest extends TestDatabaseSupport {
    @Autowired
    private EventService eventService;

    @Autowired
    private SceneEditorService editorService;

    @Autowired
    private ScenePublishService publishService;

    @Autowired
    private VenueSceneRepository sceneRepository;

    @Autowired
    private BoothRepository boothRepository;

    @Autowired
    private BoothSceneBindingRepository boothSceneBindingRepository;

    @Autowired
    private TestBusinessFixture fixture;

    @Test
    void publishesDraftAndSyncsSceneBooths() {
        var event = eventService.create(new CreateEventRequest("发布测试展会", 2029, "SCENE_DRIVEN", true));
        var savedDraft = editorService.saveDraft(event.id(), fixture.sceneDraftWithTwoBooths(), DemoActors.DEMO_ADMIN_ID);

        var published = publishService.publishDraft(
                event.id(),
                new PublishSceneRequest("首次发布"),
                DemoActors.DEMO_ADMIN_ID
        );

        var scene = sceneRepository.findByEventId(event.id()).orElseThrow();

        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(scene.getPublishedRevisionId()).isEqualTo(published.revisionId());
        assertThat(boothRepository.findByEventId(event.id()))
                .extracting("boothNo")
                .containsExactlyInAnyOrder("A01", "A02");
        assertThat(boothSceneBindingRepository.findAll())
                .filteredOn(binding -> event.id().equals(binding.getEventId()))
                .hasSize(2);
        assertThat(savedDraft.revisionId()).isEqualTo(published.revisionId());
    }
}
