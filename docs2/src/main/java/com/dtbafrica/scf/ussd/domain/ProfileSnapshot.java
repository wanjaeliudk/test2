package com.dtbafrica.scf.ussd.domain;

/**
 * The single Profile lookup performed on {@code start}. One round trip resolves both the
 * journey-1/journey-2 fork and the three-way eligibility branch.
 *
 * @param supplierId    identifier for downstream Trade finance calls; null unless known
 * @param firstName     greets returning users; null unless known
 * @param status        drives entry routing
 * @param preferredLang persisted at registration; null until then
 */
public record ProfileSnapshot(
        String supplierId,
        String firstName,
        RegistrationStatus status,
        String preferredLang) {}
