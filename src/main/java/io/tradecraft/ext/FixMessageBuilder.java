package io.tradecraft.ext;

import io.tradecraft.ext.trade.Trade;
import quickfix.field.Account;
import quickfix.field.AccountType;
import quickfix.field.ClOrdID;
import quickfix.field.ExDestination;
import quickfix.field.HandlInst;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.SecurityID;
import quickfix.field.SecurityIDSource;
import quickfix.field.SecurityType;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Builds outbound order messages from parsed rows.
 */
public final class FixMessageBuilder {
    private FixMessageBuilder() {
    }

    public static NewOrderSingle newOrderSingle(Trade row) {
        char side = FixFieldMapper.toFixSide(row.sideText);
        char ordType = FixFieldMapper.toFixOrdType(row.orderTypeText);
        char tif = FixFieldMapper.toFixTif(row.tifText);

        if (ordType == OrdType.LIMIT && row.limitPrice == null) {
            throw new IllegalArgumentException("LIMIT order missing price for " + row.symbol + " acct=" + row.account);
        }

        NewOrderSingle msg = new NewOrderSingle(
                new ClOrdID(row.clOrdId),
                new Side(side),
                new TransactTime(LocalDateTime.now(ZoneOffset.UTC)),
                new OrdType(ordType)
        );
        msg.set(new Symbol(row.symbol));
        if (row.securityId != null) { msg.set(new SecurityID(row.securityId)); }
        if (row.securityIdSrc != null) { msg.set(new SecurityIDSource(FixFieldMapper.toFixSecurityIDSource(row.securityIdSrc))); }
        if (row.securityType != null) { msg.set(new SecurityType(FixFieldMapper.toFixSecurityType(row.securityType))); }
        msg.set(new OrderQty(row.quantity));
        msg.set(new TimeInForce(tif));
        msg.set(new Account(row.account == null ? "" : row.account));
        msg.set(new AccountType(row.accountType == null ? 0 : Integer.parseInt(row.accountType)));
        if (ordType == OrdType.LIMIT) msg.set(new Price(row.limitPrice));

        if (row.exDestination != null && !row.exDestination.isEmpty()) {
            msg.set(new ExDestination(row.exDestination)); // tag 100
        }
        if (row.handlingInstruction != null && !row.handlingInstruction.isEmpty()) {
            msg.set(new HandlInst(FixFieldMapper.toFixHandlInst(row.handlingInstruction))); // tag 21
        }
        return msg;
    }

    public static OrderCancelRequest orderCancelRequest(Trade row) {
        char side = FixFieldMapper.toFixSide(row.sideText);
        OrderCancelRequest msg = new OrderCancelRequest(
                new OrigClOrdID(row.origClOrdId),
                new ClOrdID(row.clOrdId),
                new Side(side),
                new TransactTime(LocalDateTime.now(ZoneOffset.UTC))
        );
        msg.set(new Symbol(row.symbol));
        msg.set(new OrderQty(row.quantity));
        msg.set(new Account(row.account));
        return msg;
    }

    public static OrderCancelReplaceRequest orderCancelReplaceRequest(Trade row) {
        char side = FixFieldMapper.toFixSide(row.sideText);
        char ordType = FixFieldMapper.toFixOrdType(row.orderTypeText);
        char tif = FixFieldMapper.toFixTif(row.tifText);

        if (ordType == OrdType.LIMIT && row.limitPrice == null) {
            throw new IllegalArgumentException("REPLACE to LIMIT requires a price.");
        }

        OrderCancelReplaceRequest msg = new OrderCancelReplaceRequest(
                new OrigClOrdID(row.origClOrdId),
                new ClOrdID(row.clOrdId),
                new Side(side),
                new TransactTime(LocalDateTime.now(ZoneOffset.UTC)),
                new OrdType(ordType)
        );
        msg.set(new Symbol(row.symbol));
        msg.set(new OrderQty(row.quantity));
        msg.set(new TimeInForce(tif));
        msg.set(new Account(row.account));
        if (ordType == OrdType.LIMIT) msg.set(new Price(row.limitPrice));

        if (row.exDestination != null && !row.exDestination.isEmpty()) {
            msg.set(new ExDestination(row.exDestination)); // tag 100
        }
        if (row.handlingInstruction != null && !row.handlingInstruction.isEmpty()) {
            msg.set(new HandlInst(FixFieldMapper.toFixHandlInst(row.handlingInstruction))); // tag 21
        }
        return msg;
    }
}

