package io.tradecraft.ext;

import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.common.meta.Flow;
import io.tradecraft.common.meta.MessageType;
import io.tradecraft.ext.trade.Trade;
import io.tradecraft.ext.trade.TradeEntryCsvParser;
import io.tradecraft.ext.trade.TradeParser;
import io.tradecraft.fixqfj.acceptor.FixRuntimeBuilder;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.CxlRejReason;
import quickfix.field.ExecID;
import quickfix.field.LastMkt;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.OrderID;
import quickfix.field.OrigClOrdID;
import quickfix.field.SecondaryClOrdID;
import quickfix.field.SecondaryOrderID;
import quickfix.field.Text;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.Message;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReject;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

import java.time.Duration;
import java.util.List;

import static io.tradecraft.common.meta.Flow.IN;
import static io.tradecraft.common.meta.Flow.OUT;
import static io.tradecraft.common.meta.MessageType.ADMIN;
import static io.tradecraft.common.meta.MessageType.CMD;
import static io.tradecraft.common.meta.MessageType.EV;
import static io.tradecraft.common.meta.Component.CLIENT;
import static io.tradecraft.fixqfj.acceptor.FixRuntimeBuilder.Mode.INITIATOR;

/**
 * Minimal client app that sends orders from CSV and logs inbound ER/OCR.
 */
public final class TradeClient {

    private static String doubleToString(double lastPx) {
        return Double.isNaN(lastPx) ? "-" : String.format("%.4f", lastPx);
    }

    public static void main(String[] args) throws Exception {
        DualTimeSource dualTimeSource = DualTimeSource.system();
        Application app = new TradeClientApp();

        var runtime = new FixRuntimeBuilder.Builder()
                .mode(INITIATOR)
                .application(app)
                .settings("classpath:quickfix/initiator.cfg")
                .fixVersion(FixRuntimeBuilder.FixVersion.AUTO)
                .withShutdownHook(true)
                .dualTimeSource(dualTimeSource)
                .build();

        runtime.start();
        runtime.awaitLogon(Duration.ofSeconds(10));
        runtime.await();
    }

    public static void log(MessageType messageType, Flow flow, quickfix.Message m, SessionID id) {
        String raw = m.toString();
        final char SOH = 0x01;
        String pretty = raw.replace(SOH, '|');
        String template = flow == OUT ? "{}->{} {}" : "{}<-{} {}";
        LogUtils.log(CLIENT, messageType, flow, TradeClient.class, template,
                id.getSenderCompID(), id.getTargetCompID(), pretty);
    }

    static final class TradeClientApp extends MessageCracker implements Application {
        private final TradeParser tradeParser = new TradeEntryCsvParser();
        private volatile SessionID sessionID;

        @Override
        public void onCreate(SessionID id) {
        }

        @Override
        public void onLogon(SessionID id) {
            this.sessionID = id;
            String tradesFile = System.getProperty("tradesFile", "trade-entry.csv");
            List<Trade> rows =
                    tradeParser.load(tradesFile, getClass().getClassLoader());

            for (Trade row : rows) {
                Message msg;
                switch (row.tradeAction) {
                    case NEW: {
                        NewOrderSingle m = FixMessageBuilder.newOrderSingle(row);
                        LogUtils.log(CLIENT, CMD, OUT, this, FixPrettyPrinter.describeNewOrder(m));
                        msg = m;
                        break;
                    }
                    case CANCEL: {
                        OrderCancelRequest m = FixMessageBuilder.orderCancelRequest(row);
                        LogUtils.log(CLIENT, CMD, OUT, this, FixPrettyPrinter.describeCancel(m));
                        msg = m;
                        break;
                    }
                    case REPLACE:
                        OrderCancelReplaceRequest m = FixMessageBuilder.orderCancelReplaceRequest(row);
                        LogUtils.log(CLIENT, CMD, OUT, this, FixPrettyPrinter.describeReplace(m));
                        msg = m;
                        break;
                    default:
                        msg = FixMessageBuilder.newOrderSingle(row);
                        break;
                }
                FixSessionSender.send(sessionID, msg);
            }
        }

        @Override
        public void onLogout(SessionID id) {
            System.out.println("Trader logout: " + id);
        }

        @Override
        public void toAdmin(quickfix.Message message, SessionID sessionID) {

        }

        @Override
        public void fromAdmin(quickfix.Message m, SessionID id) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
            log(ADMIN, IN, m, id);
        }

        @Override
        public void toApp(quickfix.Message m, SessionID id) throws DoNotSend {
            log(EV, OUT, m, id);
        }

        @Override
        public void fromApp(quickfix.Message m, SessionID id) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
            crack(m, id);
        }

        // ----- Typed handlers -----

        // 35=8 ExecutionReport
        public void onMessage(ExecutionReport er, SessionID id) throws FieldNotFound {
            final String clOrdId = er.isSet(new ClOrdID()) ? er.getClOrdID().getValue() : "";
            final String orderId = er.isSet(new OrderID()) ? er.getOrderID().getValue() : "";
            final String execId = er.isSet(new ExecID()) ? er.getExecID().getValue() : "";
            final String secondaryClOrdId = er.isSet(new SecondaryClOrdID()) ? er.getSecondaryClOrdID().getValue() : "";
            final String secondaryOrderId = er.isSet(new SecondaryOrderID()) ? er.getSecondaryOrderID().getValue() : "";
            final String lastMkt = er.isSet(new LastMkt()) ? er.getLastMkt().getValue() : "";
            final char execType = er.getExecType().getValue();
            final char ordStat = er.getOrdStatus().getValue();

            final long lastQty = er.isSet(new LastQty()) ? (long) er.getLastQty().getValue() : 0L;
            final double lastPx = er.isSet(new LastPx()) ? er.getLastPx().getValue() : Double.NaN;
            final long cumQty = er.isSet(new CumQty()) ? (long) er.getCumQty().getValue() : 0L;
            final long leaves = er.isSet(new LeavesQty()) ? (long) er.getLeavesQty().getValue() : 0L;
            final double avgPx = er.isSet(new AvgPx()) ? er.getAvgPx().getValue() : Double.NaN;
            final String transactTime = er.isSet(new TransactTime()) ? er.getTransactTime().getValue().toString() : null;

            LogUtils.log(CLIENT, EV, IN, this,
                    "ER clOrdId={} ordId={} secondaryClOrdId={} secondaryOrderId={} execId={} execType={} ordStatus={} last={}@{} cum={} leaves={} avgPx={} lastMkt={} transactTime={}",
                    clOrdId, orderId, secondaryClOrdId, secondaryOrderId, execId, execType, ordStat, lastQty,
                    doubleToString(lastPx), cumQty, leaves, doubleToString(avgPx), lastMkt, transactTime
            );
        }

        // 35=9 OrderCancelReject
        public void onMessage(OrderCancelReject ocr, SessionID id) throws FieldNotFound {
            final String clOrdId = ocr.isSet(new ClOrdID()) ? ocr.getClOrdID().getValue() : "";
            final String origId = ocr.isSet(new OrigClOrdID()) ? ocr.getOrigClOrdID().getValue() : "";
            final String text = ocr.isSet(new Text()) ? ocr.getText().getValue() : "";
            LogUtils.log(CLIENT, EV, IN, this,
                    "OCR clOrdId={} origClOrdId={} cxlRejResponseTo={} cxlRejReason={} rejectReason={}",
                    clOrdId, origId,
                    ocr.getCxlRejResponseTo().getValue(),
                    (ocr.isSet(new CxlRejReason()) ? String.valueOf(ocr.getCxlRejReason().getValue()) : "-"),
                    text
            );
        }
    }
}
