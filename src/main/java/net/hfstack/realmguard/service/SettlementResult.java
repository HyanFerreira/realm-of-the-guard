package net.hfstack.realmguard.service;

import net.hfstack.realmguard.domain.settlement.SettlementRecord;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.Optional;

public record SettlementResult(Outcome outcome, Text feedback, SettlementRecord settlement) {
    public SettlementResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(feedback, "feedback");
    }

    public boolean succeeded() {
        return outcome == Outcome.SUCCESS;
    }

    public Optional<SettlementRecord> record() {
        return Optional.ofNullable(settlement);
    }

    public static SettlementResult success(Text feedback, SettlementRecord settlement) {
        return new SettlementResult(Outcome.SUCCESS, feedback, Objects.requireNonNull(settlement, "settlement"));
    }

    public static SettlementResult failure(Outcome outcome, Text feedback) {
        if (outcome == Outcome.SUCCESS) {
            throw new IllegalArgumentException("Use success for successful results");
        }
        return new SettlementResult(outcome, feedback, null);
    }

    public enum Outcome {
        SUCCESS,
        INVALID_NAME,
        ANCHOR_NOT_FOUND,
        ANCHOR_ALREADY_REGISTERED,
        TOO_CLOSE_TO_SETTLEMENT,
        SETTLEMENT_NOT_FOUND,
        NOT_RULER
    }
}
