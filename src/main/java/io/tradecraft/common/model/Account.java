// securityIdSource/main/java/org/example/v4/common/model/Account.java
package io.tradecraft.common.model;

public record Account(String value) implements EntityIdentifier {
    public Account {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Account cannot be blank");
    }

    @Override
    public String toString() {
        return value;
    }
}
