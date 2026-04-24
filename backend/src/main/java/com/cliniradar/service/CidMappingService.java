package com.cliniradar.service;

import com.cliniradar.entity.CidMapping;
import com.cliniradar.exception.ResourceNotFoundException;
import com.cliniradar.repository.CidMappingRepository;
import org.springframework.stereotype.Service;

@Service
public class CidMappingService {

    private final CidMappingRepository cidMappingRepository;

    public CidMappingService(CidMappingRepository cidMappingRepository) {
        this.cidMappingRepository = cidMappingRepository;
    }

    public CidMapping getByCode(String cidCode) {
        return cidMappingRepository.findByCidCodeIgnoreCase(normalize(cidCode))
                .orElseThrow(() -> new ResourceNotFoundException("CID não encontrado na base inicial: " + cidCode));
    }

    public String normalize(String cidCode) {
        return cidCode == null ? null : cidCode.trim().toUpperCase();
    }
}
