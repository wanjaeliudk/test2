package com.dtbafrica.scf.ussd.domain;

/** An offtaker the supplier delivers to. The credit limit is per (supplier, anchor). */
public record Anchor(String id, String name) {}
