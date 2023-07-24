package com.shopee.shopeegit.commit;

public enum ChangeType {
    FEAT("Features", "A new feature"),
    FIX("Bug Fixes", "A bug fix"),
    CI("Continuous Integrations", "Changes to our CI configuration files and scripts");

    public final String title;
    public final String description;

    ChangeType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String label() {
        return this.name().toLowerCase();
    }

    @Override
    public String toString() {
        return String.format("%s - %s", this.label(), this.description);
    }
}
