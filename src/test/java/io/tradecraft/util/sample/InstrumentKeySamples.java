package io.tradecraft.util.sample;


import io.tradecraft.common.domain.instrument.DomainSecurityIdSource;
import io.tradecraft.common.domain.instrument.InstrumentKey;

public class InstrumentKeySamples {
    public static final InstrumentKey AAPL = InstrumentKey.ofFix(
            "AAPL",
            "XNAS",                  // NASDAQ
            "US0378331005",          // ISIN
            DomainSecurityIdSource.ISIN
    );

    public static final InstrumentKey MSFT = InstrumentKey.ofFix(
            "MSFT",
            "XNAS",                  // NASDAQ
            "US5949181045",          // ISIN
            DomainSecurityIdSource.ISIN
    );

    public static final InstrumentKey AMZN = InstrumentKey.ofFix(
            "AMZN",
            "XNAS",                  // NASDAQ
            "US0231351067",          // ISIN
            DomainSecurityIdSource.ISIN
    );
}
