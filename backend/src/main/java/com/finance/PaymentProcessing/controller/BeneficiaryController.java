package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.*;
import com.finance.PaymentProcessing.service.BeneficiaryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {
    private final BeneficiaryService service;

    public BeneficiaryController(BeneficiaryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> addBeneficiary(@Valid @RequestBody BeneficiaryRequest request) {
        BeneficiaryResponse created = service.addBeneficiary(request);
        return ResponseEntity.created(URI.create("/api/beneficiaries/" + created.beneficiaryId())).body(created);
    }

    @GetMapping
    public List<BeneficiaryResponse> listBeneficiaries() {
        return service.listBeneficiaries();
    }

    @GetMapping("/{id}")
    public BeneficiaryResponse getBeneficiary(@PathVariable String id) {
        return service.getBeneficiary(id);
    }
}