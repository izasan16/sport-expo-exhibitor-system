package com.sportexpo.exhibitor.common;

public final class BusinessStatuses {
    public static final String ORDER_PENDING_SALES_CONFIRM = "PENDING_SALES_CONFIRM";
    public static final String ORDER_PENDING_CONTRACT = "PENDING_CONTRACT";
    public static final String ORDER_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String ORDER_PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String ORDER_PAID = "PAID";
    public static final String ORDER_COMPLETED = "COMPLETED";

    public static final String PAYMENT_PENDING_FINANCE_CONFIRM = "PENDING_FINANCE_CONFIRM";
    public static final String PAYMENT_CONFIRMED = "CONFIRMED";
    public static final String PAYMENT_CONFIRM_FAILED = "CONFIRM_FAILED";

    public static final String ELIGIBILITY_NOT_OPEN = "NOT_OPEN";
    public static final String ELIGIBILITY_AVAILABLE = "AVAILABLE";
    public static final String ELIGIBILITY_PAUSED = "PAUSED";
    public static final String ELIGIBILITY_CLOSED = "CLOSED";
    public static final String PAUSE_SELECTION_PENDING = "selection_pending";

    public static final String SELECTION_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String SELECTION_CONFIRMED = "CONFIRMED";
    public static final String SELECTION_REJECTED = "REJECTED";
    public static final String SELECTION_TIMEOUT = "TIMEOUT";

    public static final String BOOTH_AVAILABLE = "AVAILABLE";
    public static final String BOOTH_LOCKED = "LOCKED";
    public static final String BOOTH_CONFIRMED = "CONFIRMED";

    public static final String CONTRACT_DRAFT = "DRAFT";
    public static final String CONTRACT_GENERATED = "GENERATED";
    public static final String CONTRACT_EFFECTIVE = "EFFECTIVE";
    public static final String CONTRACT_VERSION_UNSIGNED = "UNSIGNED";
    public static final String CONTRACT_VERSION_MARKED_SIGNED = "MARKED_SIGNED";

    public static final String EXHIBITOR_ACTIVE = "ACTIVE";
    public static final String EXHIBITOR_SUSPENDED = "SUSPENDED";
    public static final String EXHIBITOR_ARCHIVED = "ARCHIVED";

    public static final String REGISTRATION_DRAFT = "DRAFT";
    public static final String REGISTRATION_SUBMITTED = "SUBMITTED";
    public static final String REGISTRATION_APPROVED = "APPROVED";
    public static final String REGISTRATION_REJECTED = "REJECTED";
    public static final String REGISTRATION_CANCELLED = "CANCELLED";

    public static final String LOCK_ACTIVE = "ACTIVE";
    public static final String LOCK_CONFIRMED = "CONFIRMED";
    public static final String LOCK_REJECTED = "REJECTED";
    public static final String LOCK_TIMEOUT_RELEASED = "TIMEOUT_RELEASED";

    private BusinessStatuses() {
    }
}
