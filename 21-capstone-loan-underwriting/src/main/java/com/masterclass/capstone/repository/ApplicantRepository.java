package com.masterclass.capstone.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.capstone.domain.Applicant;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ApplicantRepository {

    private final Map<String, Applicant> byId = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public ApplicantRepository(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void seed() throws IOException {
        try (var in = new ClassPathResource("data/applicants.json").getInputStream()) {
            List<Applicant> seeds = mapper.readValue(in,
                    mapper.getTypeFactory().constructCollectionType(List.class, Applicant.class));
            seeds.forEach(a -> byId.put(a.applicantId(), a));
        }
    }

    public Optional<Applicant> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
