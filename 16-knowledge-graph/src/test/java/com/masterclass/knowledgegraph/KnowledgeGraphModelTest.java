package com.masterclass.knowledgegraph;

import com.masterclass.knowledgegraph.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class KnowledgeGraphModelTest {

    @Test
    void addAndFindEntity() {
        KnowledgeGraph kg = new KnowledgeGraph();
        kg.addEntity(KgEntity.of("per:alan-turing", "Person", "Alan Turing"));

        assertThat(kg.findById("per:alan-turing")).isPresent()
                .hasValueSatisfying(e -> assertThat(e.name()).isEqualTo("Alan Turing"));
        assertThat(kg.findByType("Person")).hasSize(1);
        assertThat(kg.findByName("alan")).hasSize(1);
    }

    @Test
    void findByName_isCaseInsensitive() {
        KnowledgeGraph kg = new KnowledgeGraph();
        kg.addEntity(KgEntity.of("con:ai", "Concept", "Artificial Intelligence"));

        assertThat(kg.findByName("ARTIFICIAL")).hasSize(1);
    }

    @Test
    void traversal_multiHop() {
        KnowledgeGraph kg = new KnowledgeGraph();
        kg.addEntity(KgEntity.of("per:turing",    "Person",       "Alan Turing"));
        kg.addEntity(KgEntity.of("org:bletchley", "Organization", "Bletchley Park"));
        kg.addEntity(KgEntity.of("loc:uk",        "Location",     "United Kingdom"));

        kg.addRelation(KgRelation.of("per:turing",    "WORKS_AT",   "org:bletchley"));
        kg.addRelation(KgRelation.of("org:bletchley", "LOCATED_IN", "loc:uk"));

        List<KnowledgeGraph.TraversalResult> hops = kg.traverse("per:turing", 2);
        assertThat(hops).hasSize(2);

        List<String> reachedIds = hops.stream().map(KnowledgeGraph.TraversalResult::entityId).toList();
        assertThat(reachedIds).contains("org:bletchley", "loc:uk");
    }

    @Test
    void relationsFrom_and_relationsTo() {
        KnowledgeGraph kg = new KnowledgeGraph();
        kg.addRelation(KgRelation.of("per:turing", "INVENTED", "con:ai"));

        assertThat(kg.relationsFrom("per:turing")).hasSize(1);
        assertThat(kg.relationsTo("con:ai")).hasSize(1);
        assertThat(kg.relationsFrom("con:ai")).isEmpty();
    }
}
