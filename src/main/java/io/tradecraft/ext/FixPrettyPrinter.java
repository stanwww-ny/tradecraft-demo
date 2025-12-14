package io.tradecraft.ext;

import quickfix.FieldNotFound;
import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

/**
 * Pretty-printers for common FIX 4.4 application messages (D/F/G).
 */
public final class FixPrettyPrinter {
    private FixPrettyPrinter() {
    }

    /* =========================
       D — NewOrderSingle (35=D)
       ========================= */
    public static String describeNewOrder(NewOrderSingle msg) {
        try {
            final Side side = new Side();
            final Symbol symbol = new Symbol();
            final OrderQty qty = new OrderQty();
            final Account acct = new Account();
            final ClOrdID clOrdID = new ClOrdID();

            msg.get(side);
            msg.get(symbol);
            msg.get(qty);
            msg.get(acct);

            final String sideStr = (side.getValue() == Side.BUY) ? "BUY" : "SELL";
            final String pxStr = msg.isSetField(Price.FIELD)
                    ? "LMT@" + String.format("%.4f", msg.getPrice().getValue())
                    : "MKT";

            return String.format(
                    "NEW %s %s x%d (%s) acct=%s (ClOrdID=%s)",
                    sideStr,
                    symbol.getValue(),
                    (long) qty.getValue(),
                    pxStr,
                    acct.getValue(),
                    clOrdID.getValue()
            );
        } catch (FieldNotFound e) {
            return "NEW (missing field: " + e.getMessage() + ")";
        }
    }

    /* =========================
       F — OrderCancelRequest (35=F)
       Required typically: 41,11,55,54,60,38
       ========================= */
    public static String describeCancel(OrderCancelRequest msg) {
        try {
            final OrigClOrdID orig = new OrigClOrdID();
            final ClOrdID clid = new ClOrdID();
            final Symbol symbol = new Symbol();
            final Side side = new Side();
            final OrderQty qty = new OrderQty();
            final Account acct = new Account();

            msg.get(orig);
            msg.get(clid);
            msg.get(symbol);
            msg.get(side);
            msg.get(qty);
            if (msg.isSetField(Account.FIELD)) msg.get(acct);

            final String sideStr = (side.getValue() == Side.BUY) ? "BUY" : "SELL";
            final String acctStr = msg.isSetField(Account.FIELD) ? acct.getValue() : "-";

            return String.format(
                    "CANCEL %s %s x%d acct=%s (OrigClOrdID=%s -> ClOrdID=%s)",
                    sideStr,
                    symbol.getValue(),
                    (long) qty.getValue(),
                    acctStr,
                    orig.getValue(),
                    clid.getValue()
            );
        } catch (FieldNotFound e) {
            return "CANCEL (missing field: " + e.getMessage() + ")";
        }
    }

    /* ===========================================
       G — OrderCancelReplaceRequest (35=G)
       Typical: 41,11,55,54,60,38,40,[44 if LIMIT],59
       quantity is NEW total qty (not delta)
       =========================================== */
    public static String describeReplace(OrderCancelReplaceRequest msg) {
        try {
            final OrigClOrdID orig = new OrigClOrdID();
            final ClOrdID clid = new ClOrdID();
            final Symbol symbol = new Symbol();
            final Side side = new Side();
            final OrderQty qty = new OrderQty();
            final OrdType ordType = new OrdType();
            final TimeInForce tif = new TimeInForce();
            final Account acct = new Account();

            msg.get(orig);
            msg.get(clid);
            msg.get(symbol);
            msg.get(side);
            msg.get(qty);
            msg.get(ordType);
            if (msg.isSetField(TimeInForce.FIELD)) msg.get(tif);
            if (msg.isSetField(Account.FIELD)) msg.get(acct);

            String sideStr = (side.getValue() == Side.BUY) ? "BUY" : "SELL";
            String typeStr = switch (ordType.getValue()) {
                case OrdType.MARKET -> "MKT";
                case OrdType.LIMIT -> "LMT";
                default -> String.valueOf(ordType.getValue());
            };

            String pxStr = "-";
            if (ordType.getValue() == OrdType.LIMIT && msg.isSetField(Price.FIELD)) {
                final Price price = new Price();
                msg.get(price);
                pxStr = String.format("LMT@%.4f", price.getValue());
            }

            String tifStr = msg.isSetField(TimeInForce.FIELD) ? String.valueOf(tif.getValue()) : "-";
            String acctStr = msg.isSetField(Account.FIELD) ? acct.getValue() : "-";

            return String.format(
                    "REPLACE %s %s -> %s x%d %s TIF=%s acct=%s (OrigClOrdID=%s -> ClOrdID=%s)",
                    sideStr,
                    symbol.getValue(),
                    typeStr,
                    (long) qty.getValue(),
                    pxStr,
                    tifStr,
                    acctStr,
                    orig.getValue(),
                    clid.getValue()
            );
        } catch (FieldNotFound e) {
            return "REPLACE (missing field: " + e.getMessage() + ")";
        }
    }
}

