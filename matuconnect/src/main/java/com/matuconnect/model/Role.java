package com.matuconnect.model;


/**
 * Who a user is allowed to be in the system.
 * <p>
 * Two roles rather than three: a "planner/researcher" would read exactly the
 * same coverage and usage reports an administrator reads, so it would be a
 * role with no distinct permission of its own. If the distinction is needed
 * later it can be added without disturbing anything, because authorisation
 * is checked against the role rather than against a hard-coded username.
 */
public enum Role {

    /** Searches routes and asks the agent questions. The default on sign-up. */
    COMMUTER,

    /** Additionally manages route data and views usage reports. */
    ADMIN
}
