package net.hfstack.realmguard.domain.settlement;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum SettlementTitle {
    PROTECTOR;

    public static final Codec<SettlementTitle> CODEC = Codec.STRING.xmap(
            value -> SettlementTitle.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT)
    );

    public String translationKey() {
        return "title.realmguard." + name().toLowerCase(Locale.ROOT);
    }
}
