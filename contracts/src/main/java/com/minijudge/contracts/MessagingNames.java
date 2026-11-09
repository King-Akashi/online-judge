package com.minijudge.contracts;

public final class MessagingNames {

    public static final String EVENTS_EXCHANGE = "minijudge.events";
    public static final String DEAD_LETTER_EXCHANGE = "minijudge.dead-letter";

    public static final String SUBMISSION_REQUESTED_QUEUE = "submission.requested";
    public static final String SUBMISSION_STARTED_QUEUE = "submission.started";
    public static final String SUBMISSION_EVALUATED_QUEUE = "submission.evaluated";

    public static final String SUBMISSION_REQUESTED_ROUTING_KEY = "submission.requested";
    public static final String SUBMISSION_STARTED_ROUTING_KEY = "submission.started";
    public static final String SUBMISSION_EVALUATED_ROUTING_KEY = "submission.evaluated";

    private MessagingNames() {
    }
}
