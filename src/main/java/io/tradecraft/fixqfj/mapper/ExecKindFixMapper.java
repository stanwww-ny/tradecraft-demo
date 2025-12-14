package io.tradecraft.fixqfj.mapper;

import io.tradecraft.common.domain.market.ExecKind;
import quickfix.field.ExecType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class ExecKindFixMapper {
    private static final Map<Character, ExecKind> FIX_TO_KIND = new HashMap<>();
    private static final EnumMap<ExecKind, Character> KIND_TO_FIX = new EnumMap<>(ExecKind.class);

    static {
        map(ExecType.NEW, ExecKind.NEW);
        map(ExecType.PARTIAL_FILL, ExecKind.PARTIAL_FILL);
        map(ExecType.FILL, ExecKind.FILL);
        map(ExecType.CANCELED, ExecKind.CANCELED);
        map(ExecType.REPLACED, ExecKind.REPLACED);
        map(ExecType.REJECTED, ExecKind.REJECTED);
        map(ExecType.PENDING_CANCEL, ExecKind.PENDING_CANCEL);
        map(ExecType.PENDING_NEW, ExecKind.PENDING_NEW);
        map(ExecType.PENDING_REPLACE, ExecKind.PENDING_REPLACE);
        map(ExecType.ORDER_STATUS, ExecKind.ORDER_STATUS);
    }

    private ExecKindFixMapper() {
    }

    private static void map(char fix, ExecKind kind) {
        FIX_TO_KIND.put(fix, kind);
        KIND_TO_FIX.put(kind, fix);
    }

    /**
     * FIX char -> domain kind
     */
    public static ExecKind from(char fixValue) {
        ExecKind kind = FIX_TO_KIND.get(fixValue);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown ExecType: " + fixValue);
        }
        return kind;
    }

    /**
     * QuickFIX/J ExecType -> domain kind
     */
    public static ExecKind from(ExecType execType) {
        return from(execType.getValue());
    }

    /**
     * domain kind -> FIX char
     */
    public static char toFix(ExecKind kind) {
        Character c = KIND_TO_FIX.get(kind);
        if (c == null) {
            throw new IllegalArgumentException("No FIX mapping for ExecKind: " + kind);
        }
        return c;
    }
}
