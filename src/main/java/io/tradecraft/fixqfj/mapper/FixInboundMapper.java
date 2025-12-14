package io.tradecraft.fixqfj.mapper;

import io.tradecraft.common.domain.instrument.DomainSecurityIdSource;
import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.fixqfj.event.FixEvCancelReq;
import io.tradecraft.fixqfj.event.FixEvParentNew;
import io.tradecraft.fixqfj.event.FixEvReplaceReq;
import io.tradecraft.fixqfj.session.SessionKey;
import quickfix.CharField;
import quickfix.FieldNotFound;
import quickfix.IntField;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.Account;
import quickfix.field.AccountType;
import quickfix.field.ExDestination;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.SecurityExchange;
import quickfix.field.SecurityID;
import quickfix.field.SecurityIDSource;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

import java.math.BigDecimal;

public final class FixInboundMapper {
    private FixInboundMapper() {
    }

    public static FixEvParentNew mapNos(Message m, SessionID sid, long nowNanos) throws FieldNotFound {
        NewOrderSingle nos = (NewOrderSingle) m;
        final SessionKey sessionKey = SessionKey.of(sid);

        final String clOrdId = nos.getClOrdID().getValue();

        // --- Account(1), AccountType(581) ---
        final String accountId = getOptString(nos, Account.FIELD);
        DomainAccountType accountType = parseAccountType(getOptInt(nos, new AccountType()));

        // --- Instrument: 48/22 + 55 + 100/207 ---
        final InstrumentKey instrument = toInstrumentKey(nos);

        final char side = nos.getSide().getValue();           // tag 54 (char per FIX)
        final long qty = (long) nos.getOrderQty().getValue();// tag 38

        final char ordType = nos.getOrdType().getValue();        // tag 40
        Long limitMicros = null;
        if (ordType == '2' && nos.isSetPrice()) {
            limitMicros = toMicros(nos.getPrice().getValue());             // tag 44
        }
        if (ordType == '1') {
            limitMicros = 0L;             // tag 44
        }

        final char tif = getTif(nos);                                               // 0 means "absent" here

        String exDest = getOptString(nos, ExDestination.FIELD);

        return new FixEvParentNew(
                sessionKey, ClOrdId.of(clOrdId), accountId, accountType, instrument,
                FixOrderMappings.toDomainSide(side), qty, FixOrderMappings.toDomainOrdType(ordType),
                limitMicros, FixOrderMappings.toDomainTif(tif), exDest, nowNanos
        );
    }

    private static char getTif(NewOrderSingle nos) throws FieldNotFound {
        return nos.isSetTimeInForce()
                ? nos.getTimeInForce().getValue()                  // tag 59
                : 0;
    }

    private static char getTif(OrderCancelReplaceRequest nos) throws FieldNotFound {
        return nos.isSetTimeInForce()
                ? nos.getTimeInForce().getValue()                  // tag 59
                : 0;
    }

    public static FixEvCancelReq mapCancel(Message m, SessionID sid, long ingressNanos) throws FieldNotFound {
        OrderCancelRequest ocr = (OrderCancelRequest) m;
        final SessionKey sessionKey = SessionKey.of(sid);
        final String clOrdId = ocr.getClOrdID().getValue();
        final String origClOrdId = ocr.isSetOrigClOrdID() ? ocr.getOrigClOrdID().getValue() : null;
        final String accountId = getOptString(ocr, Account.FIELD);
        DomainAccountType accountType = parseAccountType(getOptInt(ocr, new AccountType()));
        final InstrumentKey instrument = toInstrumentKey(ocr);
        final char side = ocr.isSetSide() ? ocr.getSide().getValue() : 0;
        final Long qty = getOptLong(ocr, new OrderQty());
        return new FixEvCancelReq(
                sessionKey,
                ClOrdId.of(clOrdId),
                ClOrdId.of(origClOrdId),
                accountId,
                accountType,
                instrument,
                FixOrderMappings.toDomainSide(side),
                qty,
                ingressNanos
        );
    }

    public static FixEvReplaceReq mapReplace(Message m, SessionID sid, long ingressNanos) throws FieldNotFound {
        OrderCancelReplaceRequest ocrr = (OrderCancelReplaceRequest) m;
        final SessionKey sessionKey = SessionKey.of(sid);
        final String clOrdId = ocrr.getClOrdID().getValue();
        final String origClOrdId = ocrr.isSetOrigClOrdID() ? ocrr.getOrigClOrdID().getValue() : null;
        final String accountId = getOptString(ocrr, Account.FIELD);   // 1
        DomainAccountType accountType = parseAccountType(getOptInt(ocrr, new AccountType()));
        final InstrumentKey instrument = toInstrumentKey(ocrr);
        final DomainSide side = FixOrderMappings.toDomainSide(ocrr.isSetSide() ? ocrr.getSide().getValue() : 0);
        final Long qty = getOptLong(ocrr, new OrderQty());// tag 40
        final DomainOrdType ordType = FixOrderMappings.toDomainOrdType(ocrr.getOrdType().getValue());
        final Long limitPxMicros = toMicros(getOptPrice(ocrr, new Price()));
        final DomainTif tif = FixOrderMappings.toDomainTif(getTif(ocrr));
        final String exDest = getOptString(ocrr, ExDestination.FIELD);

        return new FixEvReplaceReq(
                sessionKey,
                ClOrdId.of(clOrdId),
                ClOrdId.of(origClOrdId),
                accountId,
                accountType,
                instrument,
                side,
                qty,
                ordType,
                limitPxMicros,
                tif,
                exDest,
                ingressNanos
        );
    }

    private static long toMicros(double px) {
        // Consistent rounding to integer micro-price
        return Math.round(px * 1_000_000d);
    }

    /* ==================================
       Instrument mapping (instrumentKey-first)
       ================================== */
    private static InstrumentKey toInstrumentKey(Message m) throws FieldNotFound {
        // Prefer SecurityID (48) + SecurityIDSource (22) when present, else Symbol (55).
        String securityId = getOptString(m, SecurityID.FIELD);
        DomainSecurityIdSource src = parseSecIdSrc(getOptString(m, SecurityIDSource.FIELD));
        String symbol = getOptString(m, Symbol.FIELD);
        String mic = firstNonBlank(
                upper(getOptString(m, ExDestination.FIELD)),
                upper(getOptString(m, SecurityExchange.FIELD))
        );
        return buildInstrument(securityId, src, symbol, mic);
    }

    private static String getOptString(Message m, int tag) {
        try {
            return m.getString(tag);
        } catch (Exception e) {
            return null;
        }
    }

    private static Character getOptChar(Message m, TimeInForce f) {
        try {
            m.getField(f);
            return f.getValue();
        } catch (Exception ignore) {
            return null;
        }
    }

    private static Integer getOptInt(Message m, IntField proto) {
        try {
            int tag = proto.getTag();
            if (m.isSetField(tag)) {
                IntField fld = new IntField(tag);
                m.getField(fld);
                return fld.getValue();
            }
            return null;
        } catch (FieldNotFound e) {
            return null;
        }
    }

    private static Long getOptLong(Message m, OrderQty f) {
        try {
            m.getField(f);
            return Math.round(f.getValue());
        } catch (Exception ignore) {
            return null;
        }
    }

    private static Double getOptPrice(Message m, Price f) {
        try {
            m.getField(f);
            return f.getValue();
        } catch (Exception ignore) {
            return null;
        }
    }

    private static Character getOptChar(Message m, CharField proto) {
        try {
            int tag = proto.getTag();
            if (m.isSetField(tag)) {
                CharField fld = new CharField(tag);
                m.getField(fld);
                return fld.getValue();
            }
            return null;
        } catch (FieldNotFound e) {
            return null;
        }
    }

    private static BigDecimal getOptDecimal(Message m, int tag) {
        try {
            if (m.isSetField(tag)) {
                return m.getDecimal(tag);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static InstrumentKey buildInstrument(String securityId, DomainSecurityIdSource src, String symbol, String mic) {
        securityId = trim(securityId);
        symbol = trim(symbol);
        mic = upper(trim(mic));
        if (securityId != null && src != DomainSecurityIdSource.UNKNOWN) {
            return new InstrumentKey(securityId, src, symbol, mic);
        }
        return new InstrumentKey(null, DomainSecurityIdSource.UNKNOWN, symbol, mic);
    }

    private static DomainAccountType parseAccountType(Integer v) {
        if (v == null) return DomainAccountType.UNKNOWN;
        return switch (v) {
            case 1 -> DomainAccountType.CUSTOMER;
            case 2 -> DomainAccountType.PRIME_BROKERAGE;
            case 3 -> DomainAccountType.HOUSE;
            case 4 -> DomainAccountType.FLOOR;
            case 6 -> DomainAccountType.INDIVIDUAL;
            default -> DomainAccountType.UNKNOWN;
        };
    }

    private static DomainSecurityIdSource parseSecIdSrc(String s) {
        if (s == null || s.isBlank()) return DomainSecurityIdSource.UNKNOWN;
        return switch (s) {
            case SecurityIDSource.CUSIP -> DomainSecurityIdSource.CUSIP;                 // "1"
            case SecurityIDSource.SEDOL -> DomainSecurityIdSource.SEDOL;                 // "2"
            case SecurityIDSource.ISIN_NUMBER -> DomainSecurityIdSource.ISIN;            // "4"
            case SecurityIDSource.RIC_CODE -> DomainSecurityIdSource.RIC;                // "5"
            case SecurityIDSource.EXCHANGE_SYMBOL -> DomainSecurityIdSource.SYMBOL; // "8"
            case SecurityIDSource.BLOOMBERG_SYMBOL -> DomainSecurityIdSource.BLOOMBERG;  // "A"
            default -> DomainSecurityIdSource.UNKNOWN;
        };
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase();
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : ((b != null && !b.isBlank()) ? b : null);
    }
}
