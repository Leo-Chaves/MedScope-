package com.cliniradar.config;

import com.cliniradar.entity.CidMapping;
import com.cliniradar.repository.CidMappingRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedDataConfig {

    private final CidMappingRepository cidMappingRepository;

    public SeedDataConfig(CidMappingRepository cidMappingRepository) {
        this.cidMappingRepository = cidMappingRepository;
    }

    @Bean
    CommandLineRunner seedCidMappings() {
        return args -> {
            List<CidMapping> seeds = List.of(
                    new CidMapping("F41.1", "Generalized anxiety disorder",
                            "generalized anxiety disorder treatment", "Mental Health"),
                    new CidMapping("M54.5", "Low back pain",
                            "low back pain treatment physiotherapy", "Musculoskeletal"),
                    new CidMapping("E11", "Type 2 diabetes mellitus",
                            "type 2 diabetes treatment", "Endocrinology"),
                    new CidMapping("L40", "Psoriasis",
                            "psoriasis treatment management", "Dermatology"),
                    new CidMapping("M32", "Systemic lupus erythematosus",
                            "systemic lupus erythematosus treatment management autoimmune disease", "Rheumatology"),
                    new CidMapping("J45", "Asthma",
                            "asthma treatment management airway inflammation", "Pulmonology"),
                    new CidMapping("F32.9", "Depressive episode",
                            "depressive disorder treatment", "Mental Health"),
                    new CidMapping("K51.9", "Ulcerative colitis",
                            "ulcerative colitis management inflammatory bowel disease", "Gastroenterology"),
                    new CidMapping("G43.9", "Migraine",
                            "migraine treatment", "Neurology")
            );

            for (CidMapping seed : seeds) {
                cidMappingRepository.findByCidCodeIgnoreCase(seed.getCidCode())
                        .orElseGet(() -> cidMappingRepository.save(seed));
            }
        };
    }
}
